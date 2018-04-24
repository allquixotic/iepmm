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

package org.github.allquixotic.iepmm.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.github.allquixotic.iepmm.IEProtectedModeManager;
import org.github.allquixotic.iepmm.RegistryType;
import org.github.allquixotic.iepmm.ZoneType;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinReg.HKEY;

import mockit.Mock;
import mockit.MockUp;

public class IEProtectedModeManagerTests {

	private IEProtectedModeManager iepmm;
	private static MyMock mocked;
	
	public static final String setName = "registrySetIntValue";
	
	@BeforeClass
	public static void setUpClass() {
		mocked = new MyMock();
	}
	
	@After
	public void teardown() {
		mocked.resetData();
		if(iepmm != null) {
			iepmm.setRestoreOnExit(false);
		}
		iepmm = null;
	}

	@Test
	public void testEnableConsistently() {

		//Arrange
		for(ZoneType zt : ZoneType.values()) {
			mocked.currState.put(zt, RegistryType.USER, false);
		}
		
		iepmm = new IEProtectedModeManager();
		iepmm.setRestoreOnExit(false);
		
		//Act
		iepmm.enableAll();
		
		//Assert
		List<RegCall> callz = mocked.calls.get(setName);
		assertThat(callz.size()).isEqualTo(4);
		RegCall curr = callz.remove(0);
		assertThat(curr.e).isEqualTo(WinReg.HKEY_CURRENT_USER);
		
		for(ZoneType zt : ZoneType.values()) {
			assertThat(iepmm.getZoneValue(zt, RegistryType.USER)).isEqualTo(true);
		}
	}
	
	@Test
	public void testRestoreOrig() {

		//Arrange
		for(ZoneType zt : ZoneType.values()) {
			mocked.currState.put(zt, RegistryType.USER, false);
		}
		
		iepmm = new IEProtectedModeManager();
		iepmm.setRestoreOnExit(false);
		iepmm.enableAll();
		
		for(ZoneType zt : ZoneType.values()) {
			assertThat(iepmm.getZoneValue(zt, RegistryType.USER)).isEqualTo(true);
		}
		
		iepmm.restoreOrigState();
		
		//Assert
		
		for(ZoneType zt : ZoneType.values()) {
			assertThat(iepmm.getZoneValue(zt, RegistryType.USER)).isEqualTo(false);
		}
	}
	
	static class MyMock extends RegCallMockUp<Advapi32Util> {

		final Table<ZoneType, RegistryType, Boolean> currState = ArrayTable
				.create(Arrays.asList(ZoneType.values()), Arrays.asList(RegistryType.values()));
		
		@Mock
		void registrySetIntValue(HKEY e, String path, int data) {
			List<RegCall> callz = calls.get(setName);
			if(callz == null) {
				callz = new ArrayList<RegCall>();
				calls.put(setName, callz);
			}
			callz.add(new RegCall(e,path,data));
			
			RegistryType rt = hkeyToRt(e);
			ZoneType zt = pathToZone(path);
			boolean enabled = data == 0;
			currState.put(zt, rt, enabled);
		}
		
		@Mock
		int registryGetIntValue(HKEY e, String path) {
			if(e == null || path == null) {
				throw new RuntimeException();
			}
			RegistryType rt = hkeyToRt(e);
			ZoneType zt = pathToZone(path);
			
			Boolean b = currState.get(zt, rt);
			if(b != null && b == true) {
				return 0;
			}
			if(b != null && b == false) {
				return 3;
			}
			throw new RuntimeException("The system could not find the file specified.");
		}
		
		private static ZoneType pathToZone(String path) {
			ZoneType zt = null;
			switch(path) {
			case "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\1\\2500":
				zt = ZoneType.INTRANET;
				break;
			case "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\2\\2500":
				zt = ZoneType.TRUSTED;
				break;
			case "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\3\\2500":
				zt = ZoneType.INTERNET;
				break;
			case "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\4\\2500":
				zt = ZoneType.RESTRICTED;
				break;
			}
			return zt;
		}
		
		private static RegistryType hkeyToRt(HKEY e) {
			return (e.equals(WinReg.HKEY_CURRENT_USER) ? RegistryType.USER : RegistryType.SYSTEM);
		}

		@Override
		void resetData() {
			calls.clear();
			for(ZoneType zt : ZoneType.values()) {
				for(RegistryType rt : RegistryType.values()) {
					currState.put(zt, rt, null);
				}
			}
		}
	}
	
	static abstract class RegCallMockUp<T> extends MockUp<T> {
		final Map<String, List<RegCall>> calls = new HashMap<String, List<RegCall>>();
		abstract void resetData();
	}
	
	static class RegCall {
		public HKEY e;
		public String path;
		public int data;
		
		public RegCall(HKEY e, String path, int data) {
			this.e = e;
			this.path = path;
			this.data = data;
		}
	}
}
