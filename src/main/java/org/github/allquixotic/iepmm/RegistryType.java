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

import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinReg.HKEY;

public enum RegistryType {
	USER(1), SYSTEM(2);
	
	private final int val;
	
	RegistryType(int val) {
		this.val = val;
	}
	
	public int getNum() {
		return val;
	}
	
	public HKEY toHkey() {
		if(val == 1) {
			return WinReg.HKEY_CURRENT_USER;
		}
		if(val == 2) {
			return WinReg.HKEY_LOCAL_MACHINE;
		}
		return null;
	}
	
	public static HKEY typeToHkey(RegistryType input) {
		return input.toHkey();
	}
	
	public static RegistryType hkeyToType(HKEY key) {
		if(key.equals(WinReg.HKEY_CURRENT_USER)) {
			return RegistryType.USER;
		}
		if(key.equals(WinReg.HKEY_LOCAL_MACHINE)) {
			return RegistryType.SYSTEM;
		}
		return null;
	}
}
