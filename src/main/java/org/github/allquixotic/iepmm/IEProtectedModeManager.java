/*Copyright 2018 Sean McNamara <smcnam@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package org.github.allquixotic.iepmm;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;
import com.sun.jna.platform.win32.Advapi32Util;

public class IEProtectedModeManager {

	private static final Lock callLock = new ReentrantLock();
	private static final AtomicBoolean alreadyInited = new AtomicBoolean(false);
	private static final AtomicBoolean restoreOnExit = new AtomicBoolean(true);
	private static final Table<ZoneType, RegistryType, Boolean> origState = ArrayTable
			.create(Arrays.asList(ZoneType.values()), Arrays.asList(RegistryType.values()));

	public IEProtectedModeManager() {
		initialize();
	}

	private void initialize() {
		try {
			callLock.lock();
			if(!alreadyInited.get()) {				
				refreshTable(origState);
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							callLock.lock();
							if (restoreOnExit.get()) {
								restoreOrigState();
							}
						} catch (Throwable t) {
							callLock.unlock();
						}
					}
				});
				alreadyInited.set(true);
			}
		} catch (Throwable t) {
			throw t;
		} finally {
			callLock.unlock();
		}
	}

	private void refreshTable(Table<ZoneType, RegistryType, Boolean> data) {
		try {
			callLock.lock();
			for (RegistryType rt : RegistryType.values()) {
				for (ZoneType zt : ZoneType.values()) {
					Boolean zoneval = getZoneValue(zt, rt);
					data.put(zt, rt, zoneval);
				}
			}
		} catch (Throwable t) {
			throw t;
		} finally {
			callLock.unlock();
		}
	}

	// High-level public API.

	public boolean enableAll() {
		return enableAll(true, false);
	}

	public boolean disableAll() {
		return disableAll(true, false);
	}

	public boolean enableForRiskyOnly() {
		return enableForRiskyOnly(true, false);
	}

	public boolean enableAll(boolean user, boolean system) {
		try {
			callLock.lock();
			boolean allWorked = true;
			for (ZoneType zt : ZoneType.values()) {
				if (user) {
					allWorked = allWorked && setZoneValue(zt, RegistryType.USER, true);
				}
				if (system) {
					allWorked = allWorked && setZoneValue(zt, RegistryType.SYSTEM, true);
				}
			}
			return allWorked;
		} catch (Throwable t) {
			throw t;
		} finally {
			callLock.unlock();
		}
	}

	public boolean disableAll(boolean user, boolean system) {
		try {
			callLock.lock();
			boolean allWorked = true;
			for (ZoneType zt : ZoneType.values()) {
				if (user) {
					allWorked = allWorked && setZoneValue(zt, RegistryType.USER, false);
				}
				if (system) {
					allWorked = allWorked && setZoneValue(zt, RegistryType.SYSTEM, false);
				}
			}
			return allWorked;
		} catch (Throwable t) {
			throw t;
		} finally {
			callLock.unlock();
		}
	}

	public boolean enableForRiskyOnly(boolean user, boolean system) {
		try {
			callLock.lock();
			boolean allWorked = true;
			if (user) {
				allWorked = allWorked && setZoneValue(ZoneType.INTERNET, RegistryType.USER, true);
				allWorked = allWorked && setZoneValue(ZoneType.RESTRICTED, RegistryType.USER, true);
				allWorked = allWorked && setZoneValue(ZoneType.INTRANET, RegistryType.USER, false);
				allWorked = allWorked && setZoneValue(ZoneType.TRUSTED, RegistryType.USER, false);
			}
			if (system) {
				allWorked = allWorked && setZoneValue(ZoneType.INTERNET, RegistryType.SYSTEM, true);
				allWorked = allWorked && setZoneValue(ZoneType.RESTRICTED, RegistryType.SYSTEM, true);
				allWorked = allWorked && setZoneValue(ZoneType.INTRANET, RegistryType.SYSTEM, false);
				allWorked = allWorked && setZoneValue(ZoneType.TRUSTED, RegistryType.SYSTEM, false);
			}
			return allWorked;
		} catch (Throwable t) {
			throw t;
		} finally {
			callLock.unlock();
		}
	}

	public Table<ZoneType, RegistryType, Boolean> restoreOrigState() {
		try {
			callLock.lock();
			for (RegistryType rt : RegistryType.values()) {
				for (ZoneType zt : ZoneType.values()) {
					Boolean origSt = origState.get(zt, rt);
					if(origSt != null) {
						setZoneValue(zt, rt, origSt);
					}
				}
			}
			Table<ZoneType, RegistryType, Boolean> retval = ArrayTable.create(Arrays.asList(ZoneType.values()),
					Arrays.asList(RegistryType.values()));
			refreshTable(retval);
			return retval;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		} finally {
			callLock.unlock();
		}
	}

	public void setRestoreOnExit(boolean doRestoreOnExit) {
		restoreOnExit.set(doRestoreOnExit);
	}

	// Low-level public API.

	public Boolean getZoneValue(ZoneType zone, RegistryType regType) {
		if (zone == null || regType == null) {
			throw new NullPointerException();
		}
		try {
			callLock.lock();
			int ret = Advapi32Util.registryGetIntValue(regType.toHkey(), String.format(Consts.REG_PATH, zone.getVal()));
			if (ret == 3) {
				return false;
			}
			if (ret == 0) {
				return true;
			}
		} catch (Throwable e) {
			if (e instanceof RuntimeException) {
				System.err.println("[IEProtectedModeManager] INFO: getZoneValue() failed with: ");
				System.err.println("\tzone: " + zone.toString());
				System.err.println("\tregistryType: " + regType.toString());
				System.err.println("\tThe error message was: " + e.getMessage());
			} else {
				e.printStackTrace();
			}
		} finally {
			callLock.unlock();
		}
		return null;
	}

	public Boolean setZoneValue(ZoneType zone, RegistryType reg, Boolean enabled) {
		if (zone == null || reg == null || enabled == null) {
			throw new NullPointerException();
		}
		try {
			callLock.lock();
			Advapi32Util.registrySetIntValue(reg.toHkey(), String.format(Consts.REG_PATH, zone.getVal()),
					enabled ? 0 : 3);
			return true;
		} catch (Throwable we) {
			System.err.println("[IEProtectedModeManager] INFO: setZoneValue failed with:");
			System.err.println("\tzone: " + zone.toString());
			System.err.println("\tregistryType: " + reg.toString());
			System.err.println("\tenabled: " + reg.toString());
			System.err.println("\tThe error message was: " + we.getMessage());
			return false;
		} finally {
			callLock.unlock();
		}
	}

}
