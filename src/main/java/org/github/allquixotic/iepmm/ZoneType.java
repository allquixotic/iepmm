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

public enum ZoneType {
	INTRANET(1), TRUSTED(2), INTERNET(3), RESTRICTED(4);

	private final int val;

	ZoneType(int val) {
		this.val = val;
	}

	public int getVal() {
		return val;
	}

	public static ZoneType valToZone(int val) {
		switch (val) {
		case 1:
			return INTRANET;
		case 2:
			return TRUSTED;
		case 3:
			return INTERNET;
		case 4:
			return RESTRICTED;
		default:
			return null;
		}
	}
	
	public static int zoneToVal(ZoneType z) {
		return z.getVal();
	}
}
