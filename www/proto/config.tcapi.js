//globals: equal, responseText, statement, ok, deepEqual, QUnit, module, asyncTest, Util, start, golfStatements, console
/*jslint bitwise: true, browser: true, plusplus: true, maxerr: 50, indent: 4 */
function Config() {
	"use strict";
}
Config.endpoint = "https://cloud.scorm.com/ScormEngineInterface/TCAPI/public/";
Config.authUser = "test";
Config.authPassword = "password";
Config.actor = { "mbox":["jomofrodo@gmail.com"], "name":["Jomo Frodo"] };
Config.registration = "<registration-uuid>";
