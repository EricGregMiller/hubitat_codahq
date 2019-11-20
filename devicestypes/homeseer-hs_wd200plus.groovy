/**
 *  HomeSeer HS-WD200+ Dimmer
 *
 *  Copyright 2019 Eric G. Miller
 *
 *  Modified from Ben Rimmasch HomeSeer HS-WD200+ Dimmer
 *  Modified from the work by DarwinsDen device handler for the WD100 version 1.03 and from the work by HomeSeer for the HS-WD200+
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Author: Eric G. Miller
 *	Date: 2019-11-16
 *  Github: https://github.com/EricGregMiller/hubitat_codahq
 *  Source: Fork from https://github.com/codahq/hubitat_codahq.
 *
 *  Changelog
 *  ---------
 *  2019-11-19 Emiller
 *    * Fork from https://github.com/codahq/hubitat_codahq
 *    * Move constants and special knowledge to front of code. Most notable: the button definitions and parameters are now coded as maps.
 *    * Factorize tap response code. Thus replaced all the tapUp/DownNResponse methods with a single method with arguments.
 *        Allowed removal of giant switch-case in zwaveEvent method.
 *    * Remove all sendEvent calls from event handlers. Instead add such events to the return list of parse.
 *        (1) This seems to be the proper use for the Hubitat/Smarthings design
 *        (2) I don't know specifics for Hubitat, but in general it's a bad idea to have long delays in methods called from a server.
 *        (3) Doing this fixed the double-tap to a level option. Without it that option did not work for me.
 *    * Upgrade the single-tap up and the double-tap up and down options to set to any level. Add option for double-tap to move level up or down.
 *    * Change LED status to list. Allows for cleaner code and less switch-case statements.
 *    * Replace blink switch and hardcoded masks with formula based on switch index.
 *    * Change LED status to be more readable. A single list holds colors and blink. Requires a little extra storage.
 *
 *
 *  Ben Rimmasch Changelog
 *  ----------------------
 *  1.0       2019-06-07 Initial Hubitat Version
 *  1.0.1     2019-06-08 Fixes for some hubs not liking BigDecimals passed as configurationValue
 *  1.0.2     2019-06-09 Small fix so that when setting LED colors on a fan and a dimmer 0 can be used for all as well as 8
 *  1.0.3     2019-09-12 Fixed the delay between level gets in setLevel
 *
 *
 *	Previous Driver's Changelog
 *  ---------------------------
 *	1.0.dd.9  13-Feb-2019 Added dummy setLevel command with duration for compatibility with HA Bridge, others? (darwin@darwinsden.com)
 *	1.0.dd.8  28-Jul-2018 Additional protection against floating point default preference values
 *	1.0.dd.6  27-Jul-2018 Added call to set led flash rate and added protection against floating point default preference values
 *	1.0.dd.5  26-Mar-2018 Corrected issues: 1) Turning off all LEDs did not return switch to Normal mode,
 *                        2) Turning off last lit LED would set Normal mode, but leave LED state as on (darwin@darwinsden.com)
 *	1.0.dd.4  28-Feb-2018 Updated all LED option to use LED=0 (8 will be depricated) and increased delay by 50ms (darwin@darwinsden.com)
 *	1.0.dd.3  19-Feb-2018 Corrected bit-wise blink off operator (darwin@darwinsden.com)
 *	1.0.dd.2  16-Feb 2018 Added button number labels to virtual buttons and reduced size (darwin@darwinsden.com)
 *	1.0.dd.1  15-Feb 2018 Added option to set all LED's simultaneously(darwin@darwinsden.com)
 *	1.0	      Jan    2017 Initial Version
 *
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        pushed
 *   Double-Tap Down   2        pushed
 *   Triple-Tap Up     3        pushed
 *   Triple-Tap Down   4        pushed
 *   Hold Up           5        pushed
 *   Hold Down         6        pushed
 *   Single-Tap Up     7        pushed
 *   Single-Tap Down   8        pushed
 *   4 taps up         9        pushed
 *   4 taps down       10       pushed
 *   5 taps up         11       pushed
 *   5 taps down       12       pushed
 *
 */

/**
 * Scene values returned with switch events
 */
private static Map scenes() {
   return [up: 1, down: 2]
}

/**
 * Key attribute values returned with switch events
 */
private static Map ka() {
   [
     tap1: 0,
     unknown: 1,
     hold: 2,
     tap2: 3,
     tap3: 4,
     tap4: 5,
     tap5: 6
   ]
}

/**
 * Special value for hold "number of taps"
 */
private static int hold() { -1 }

/**
 * Association of buttons numbers with direction, taps, scene, and key attribute
 * It should be possible to change button number simply by editing this map.
 */
private static List<Map> buttons() {
  [
    [buttonNum: 1, direction: "Up",    numTaps: 2,      scene: scenes().up,   keyAttribute: ka().tap2],
    [buttonNum: 2, direction: "Down",  numTaps: 2,      scene: scenes().down, keyAttribute: ka().tap2],
    [buttonNum: 3, direction: "Up",    numTaps: 3,      scene: scenes().up,   keyAttribute: ka().tap3],
    [buttonNum: 4, direction: "Down",  numTaps: 3,      scene: scenes().down, keyAttribute: ka().tap3],
    [buttonNum: 5, direction: "Up",    numTaps: hold(), scene: scenes().up,   keyAttribute: ka().hold],
    [buttonNum: 6, direction: "Down",  numTaps: hold(), scene: scenes().down, keyAttribute: ka().hold],
    [buttonNum: 7, direction: "Up",    numTaps: 1,      scene: scenes().up,   keyAttribute: ka().tap1],
    [buttonNum: 8, direction: "Down",  numTaps: 1,      scene: scenes().down, keyAttribute: ka().tap1],
    [buttonNum: 9, direction: "Up",    numTaps: 4,      scene: scenes().up,   keyAttribute: ka().tap4],
    [buttonNum: 10, direction: "Down", numTaps: 4,      scene: scenes().down, keyAttribute: ka().tap4],
    [buttonNum: 11, direction: "Up",   numTaps: 5,      scene: scenes().up,   keyAttribute: ka().tap5],
    [buttonNum: 12, direction: "Down", numTaps: 5,      scene: scenes().down, keyAttribute: ka().tap5],
  ]
}

// Standard says max level is 100. But original code only allowed 99 and my testing indicates that 99 is highest.
private static int maxSwitchLevel() { 99 }

/**
 * Number of status LED. LEDs are numbered from 1 to max where 1 is the low value, often called "bottom" and max
 * the high value "top". There is a reverse setting for the swtich were low is the top and high is the bottom. The
 * status LEDs also switch so 1 becomes the physical top and max the physical bottom.
 */
private static int numLeds() { 7 }
// Maximum LED blink value on switch
private static int maxBlinkPeriod() { 255 }
// Conversion factor from switch blink value to ms (Each value is 100ms.)
private static int blinkMsPerValue() { 100 }
// Maximum LED blink value in ms (used for user interface)
private static int maxBlinkPeriodMs() { maxBlinkPeriod() * blinkMsPerValue() }
// Default blink period. Used when blink is requested and no period is set. (5 = 500ms)
private static int defaultBlinkPeriod() { 5 }

/**
 * Parameters used by switch to control switch behavior LEDs
 */
private static Map params() {
    [
      ledOff:                 3, // 0: LED on when switch off, 1: LED off when off
      loadOrientation:        4, // 0: Top of paddle is on, 1: Bottom of paddle is on
      configVerify:       7..10, // From code
      remoteControlRampRate: 11, // Set dimmer Ramp rate for remote control. Range 0 - 90 seconds. From existing code and older doc. Not in current doc.
      localControlRampRate:  12, // Set dimmer Ramp rate for local control. Range 0 - 90 seconds. From existing code and older doc. Not in current doc.
      ledOperation:          13, // 0: Normal Mode (show switch level), 1: Custom or Status Mode (customize LEDs)
      ledNormalColor:        14, // Sets normal color of LED. See colors.
      ledStatusColors:   21..27, // Set status color for each LED. See colors.
      ledBlinkPeriod:        30, // LED blink period (1/frequency) in tenths of seconds (e.g. 1 = 100ms, 2 = 200ms, ... 255 = 25500ms)
      ledBlinkMask:          31  // LED blink mask. A byte that defines which LEDs blink. (0x01 = LED 1, 0X02 = LED 2, etc)
    ]
}

/**
 * LED status colors
 */
private static List<String> colors() {
  ["Off", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan", "White"]
}

/**
 * Color number of a status color name
 */
private static int color(colorName) {
  colors().indexOf(colorName)
}

/**
 * LED normal mode colors. Slightly different than status because no off and white is zero.
 */
private static List normalColors() {
  List normalColors = []
  normalColors << colors()[-1]
  (1..colors().size() - 2).each {ii -> normalColors << colors()[ii]}
  return normalColors
}

/**
 * Color number of a normal color name
 */
private static int normalColor(colorName) {
  normalColors().indexOf(colorName)
}

/**
* Standard device handler metadata
*/
metadata {
  definition(name: "HS-WD200+ Dimmer Emiller", namespace: "codahq-hubitat", author: "Eric Miller",
    importUrl: "https://raw.githubusercontent.com/codahq/hubitat_codahq/master/devicestypes/homeseer-hs_wd200plus.groovy") {
    capability "Switch Level"
    capability "Actuator"
    capability "Indicator"
    capability "Switch"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "PushableButton"
    capability "Configuration"

    command "tapUp2"
    command "tapDown2"
    command "tapUp3"
    command "tapDown3"
    command "tapUp4"
    command "tapDown4"
    command "tapUp5"
    command "tapDown5"
    command "holdUp"
    command "holdDown"
    command "setStatusLed", [
      [name: "LED*", type: "STRING", description: "Comma separated list of LED numbers, where 1 is bottom LED and ${numLeds()} is top. Use '..' to specify a range. Thus '1,2..5,7' sets LEDs 1,2,3,4,5, and 7. To set all use '1..' or '..7'"],
      [name: "Color", type: "ENUM", constraints: colors(), description: "Select LED Color", default: "Off"],
      [name: "Blink", type: "ENUM", constraints: ["No", "Yes"], description: "Make LED blink?"]
    ]
    command "setSwitchModeNormal"
    command "setSwitchModeStatus"
    command "setNormalModeLedColor", [[name: "Set Normal Mode LED Color", type: "ENUM", constraints: normalColors(), description:
        "Select LED Color for Normal Mode"]]
    command "setBlinkDurationMS", [[name: "Set Blink Duration", type: "NUMBER", description: "Milliseconds (0 to ${maxBlinkPeriodMs()})"]]

    fingerprint mfr: "000C", prod: "4447", model: "3036"
    //to add new fingerprints convert dec manufacturer to hex mfr, dec deviceType to hex prod, and dec deviceId to hex model
  }

  simulator {
    status "on": "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"
    status "09%": "command: 2003, payload: 09"
    status "10%": "command: 2003, payload: 0A"
    status "33%": "command: 2003, payload: 21"
    status "66%": "command: 2003, payload: 42"
    status "99%": "command: 2003, payload: 63"

    // reply messages
    reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
    reply "200100,delay 5000,2602": "command: 2603, payload: 00"
    reply "200119,delay 5000,2602": "command: 2603, payload: 19"
    reply "200132,delay 5000,2602": "command: 2603, payload: 32"
    reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
    reply "200163,delay 5000,2602": "command: 2603, payload: 63"
  }

  preferences {
    input "singleTapToLevel", "bool", title: "Single-Tap Up sets to level", defaultValue: false, displayDuringSetup: true, required: false
    input("singleTapLevel", "number", title: "Single-Tap Up Level (1-${maxSwitchLevel()})", range: "1..${maxSwitchLevel()}", required: false)
    input("doubleTapUpAction", "enum", title: "Action On Double-Tap Up", options: ["Set to level", "Increase by amount"], description: "Select Action", displayDuringSetup: true, required: false)
    input("doubleTapUpLevel", "number", title: "Double-Tap Up Level or change (1-${maxSwitchLevel()})", range: "1..${maxSwitchLevel()}", required: false)
    input("doubleTapDownAction", "enum", title: "Action On Double-Tap Down", options: ["Set to level", "Decrease by amount"], description: "Select Action", displayDuringSetup: true, required: false)
    input("doubleTapDownLevel", "number", title: "Double-Tap Down Level or change (1-${maxSwitchLevel()})", range: "1..${maxSwitchLevel()}", required: false)
    input "reverseSwitch", "bool", title: "Reverse Switch", defaultValue: false, displayDuringSetup: true, required: false
    input "bottomled", "bool", title: "Bottom LED On if Load is Off", defaultValue: false, displayDuringSetup: true, required: false
    input("localcontrolramprate", "number", title: "Press Configuration button after changing preferences\n\nLocal Ramp Rate: Duration (0-90)(1=1 sec) [default: 3]", defaultValue: 3, range: "0..90", required: false)
    input("remotecontrolramprate", "number", title: "Remote Ramp Rate: duration (0-90)(1=1 sec) [default: 3]", defaultValue: 3, range: "0..90", required: false)
    input("color", "enum", title: "Default Normal Mode LED Color", options: normalColors(), description: "Select LED Color for Normal Mode", required: false)
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }

}

/**
 * Method which handles all messages from switch
*/
List parse(String description) {
  def result = []
  def cmd = null
  logDebug("parse($description)")
  if (description != "updated") {
    // Use version 1 of 0x20 (Basic), 26 (SwitchMultilevel), and 70 (Configuration)
    cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
    logTrace "cmd: $cmd"
    if (cmd) {
      result = zwaveEvent(cmd)
    }
  }
  if (result) {
    logDebug "Parsed ${cmd} to result ${result.inspect()}"
  } else {
    log.warn "Parse returned empty result for command ${cmd}"
  }
  return result
}

List zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
  dimmerEvents(cmd)
}

List zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
  dimmerEvents(cmd)
}

List zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
  dimmerEvents(cmd)
}

List zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd) {
  dimmerEvents(cmd)
}

private List dimmerEvents(hubitat.zwave.Command cmd) {
  logDebug "dimmerEvents(hubitat.zwave.Command cmd)"
  logTrace "cmd: $cmd"
  String value = (cmd.value ? "on" : "off")
  List result = [createEvent(name: "switch", value: value)]
  logInfo "Switch for ${device.label} is ${value}"
  if (cmd.value != null) {
    state.lastLevel = cmd.value < 0 ? 0 : cmd.value > maxSwitchLevel() ? maxSwitchLevel() : cmd.value
    result << createEvent(name: "level", value: cmd.value, unit: "%")
    logInfo "Level for ${device.label} is ${cmd.value}"
  }
  return result
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd)"
  logTrace "cmd: $cmd"
  String value = "when off"
  if (cmd.configurationValue[0] == 1) { value = "when on" }
  if (cmd.configurationValue[0] == 2) { value = "never" }
  logInfo "Indicator is on for fan: ${value}"
  createEvent([name: "indicatorStatus", value: value])
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd)"
  logTrace "cmd: $cmd"
  logInfo "Switch button was pressed"
  createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd)"
  logTrace "cmd: $cmd"
  logDebug "manufacturerId:   ${cmd.manufacturerId}"
  logDebug "manufacturerName: ${cmd.manufacturerName}"
  logDebug "productId:        ${cmd.productId}"
  logDebug "productTypeId:    ${cmd.productTypeId}"
  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  def cmds = []
  if (!(msr.equals(getDataValue("MSR")))) {
    updateDataValue("MSR", msr)
    cmds << createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: true, displayed: false])
  }
  if (!(cmd.manufacturerName.equals(getDataValue("manufacturer")))) {
    updateDataValue("manufacturer", cmd.manufacturerName)
    cmds << createEvent([descriptionText: "$device.displayName manufacturer: $msr", isStateChange: true, displayed: false])
  }
  cmds
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd)"
  logTrace "cmd: $cmd"
  logDebug("received Version Report")
  logDebug "applicationVersion:      ${cmd.applicationVersion}"
  logDebug "applicationSubVersion:   ${cmd.applicationSubVersion}"
  logDebug "zWaveLibraryType:        ${cmd.zWaveLibraryType}"
  logDebug "zWaveProtocolVersion:    ${cmd.zWaveProtocolVersion}"
  logDebug "zWaveProtocolSubVersion: ${cmd.zWaveProtocolSubVersion}"
  def ver = cmd.applicationVersion + '.' + cmd.applicationSubVersion
  def cmds = []
  if (!(ver.equals(getDataValue("firmware")))) {
    updateDataValue("firmware", ver)
    cmds << createEvent([descriptionText: "Firmware V" + ver, isStateChange: true, displayed: false])
  }
  cmds
}

def zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd)"
  logTrace "cmd: $cmd"
  logDebug("received Firmware Report")
  logDebug "checksum:       ${cmd.checksum}"
  logDebug "firmwareId:     ${cmd.firmwareId}"
  logDebug "manufacturerId: ${cmd.manufacturerId}"
  [:]
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd)"
  logTrace "cmd: $cmd"
  logInfo "Stop level change on device ${device.label}"
  [createEvent(name: "switch", value: "on"), response(zwave.switchMultilevelV1.switchMultilevelGet().format())]
}

def zwaveEvent(hubitat.zwave.Command cmd) {
  // Handles all Z-Wave commands we aren't interested in
  logDebug "zwaveEvent(hubitat.zwave.Command cmd)"
  logTrace "cmd: $cmd"
  [: ]
}

/*
//TODO
indicatorNever()
indicatorWhenOff()
indicatorWhenOn()
*/

private def setLevelDeviceCommands(level, dimmer) {
  state.lastLevel = level
  def cmds = [
    zwave.basicV1.basicSet(value: level).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ]
  if (dimmer) {
    cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
  }
  delayBetween(cmds, 5000)
}

def on() {
  logDebug "on()"
  buttonResponse("digital", 1, "Up").each { sendEvent(it) }
  setLevelDeviceCommands(0xFF, false)
}

def off() {
  logDebug "off()"
  buttonResponse("digital", 1, "Down").each { sendEvent(it) }
  setLevelDeviceCommands(0x00, false)
}

private def setLevelComputeLevel(value) {
  logDebug "setLevelComputeLevel($value)"
  def valueaux = value as Integer
  def level = valueaux < 0 ? 0 : valueaux > maxSwitchLevel() ? maxSwitchLevel() : valueaux
  logDebug "computed Level: $level"
  return level
}

private def setLevelEventMaps(level) {
  logDebug "setLevelEventMaps Level: $level"
  def onoff = level > 0 ? "on" : "off"
  return [
    [name: "switch", value: onoff],
    [name: "level", value: level, unit: "%"]
  ]
}

// Switch level interface method
def setLevel(value) {
  def level = setLevelComputeLevel(value)
  setLevelEventMaps(level).each {
    sendEvent(it)
  }
  setLevelDeviceCommands(level, true)
}

private def setLevelNoSend(value) {
  def level = setLevelComputeLevel(value)
  def result = setLevelEventMaps(level).collect { createEvent(it) }
  result.addAll(response(setLevelDeviceCommands(level, true)))
  return result
}

// dummy setLevel command with duration for compatibility with Home Assistant Bridge (others?)
def setLevel(value, duration) {
  logDebug "setLevel(value, duration)"
  setLevel(value)
}

private def changeLevelNoSend(value) {
  logDebug "changeLevelNoSend($value)"
  def lastLevel = 0
  if (state.lastLevel) {
    lastLevel = state.lastLevel
  }
  return setLevelNoSend(lastLevel + value)
}

/*
 *  Set dimmer to status mode, then set the color of the individual LED
 *
 *  led = 1-7
 *  color = 0=0ff
 *  1=red
 *  2=green
 *  3=blue
 *  4=magenta
 *  5=yellow
 *  6=cyan
 *  7=white
 */

def setBlinkDurationMS(newBlinkDuration) {
  logDebug "setBlinkDurationMS($newBlinkDuration)"
  def cmds = []
  if (0 < newBlinkDuration && newBlinkDuration < maxBlinkPeriodMs()) {
    logDebug "setting blink duration to: ${newBlinkDuration} ms"
    state.blinkDuration = newBlinkDuration.toInteger() / blinkMsPerValue()
    logDebug "blink duration config (parameter ${params().ledBlinkPeriod}) is: ${state.blinkDuration}"
    cmds << zwave.configurationV2.configurationSet(configurationValue: [state.blinkDuration.toInteger()],
      parameterNumber: params().ledBlinkPeriod, size: 1).format()
  } else {
    log.warn "commanded blink duration ${newBlinkDuration} is outside range 0 .. ${maxBlinkPeriodMs()} ms"
  }
  return cmds
}

/**
 * Convert comma separated string of integers to a list. Use '..' to specify a range.
 * Thus '1,3..6' produces [1,3,4,5,6]. '1..3,4,7' produces [1,2,3,4,7].
 * Defining one end of range assume going to end. Thus '..3' produces [1,2,3] and '4..' produces [4,5, ... ,max]
 * A warning is logged for invalid values and they are ignored.
 */
private List stringToInts(String intString, int max) {
  logDebug "stringToInts string $intString, max $max"
  if (!intString) {
    return
  }
  def commaSplit = ~/ *, */
  def rangeSplit = ~/ *\.\.+ */
  List values = commaSplit.split(intString)
  logDebug "values $values"
  result = []
  values.each {
    logTrace "processing value $it"
    result.addAll(stringRangeToInts(it, max, rangeSplit))
  }
  return result.sort()
}

/**
 * Convert a string which defines an integer range to a list of integers. A range is indicated by two dots '..'.
 * Thus '3..6' returns [3,4,5,6]. If the beginning value is missing the first bound is one (1). Thus '..4' returns [1,2,3,4]
 * Likewise when the final value is missing it is assumed to be 'max'. Thus '3..' returns [3,4, ... ,max]
 * A single integer without a range indicator return a list with a single value. Thus '5' returns [5]
 * Returned integers are between one (1) and input 'max'. Values less than one or greater than 'max' are not returned.
 *
 * @param rangeString String containing input range.
 * @param max Maximum value allowed.
 * @param rangeSplit A regex pattern which defines the range splitter. It is input to avoid the overhead of compiling
 *        the regex many times.
 */
private List stringRangeToInts(String rangeString, int max, rangeSplit) {
  logTrace "processing value $rangeString"
  List result = []
  Integer startValue = null
  Integer endValue = null
  def matcher = rangeString =~ rangeSplit
  if (matcher.find()) {
    def endpoints = rangeSplit.split(rangeString)
        .findAll { it != null && it != "" && it.isInteger() }
        .collect { it.toInteger() }
    logTrace "endpoints $endpoints"
    if (endpoints.size() == 0) {
    } else if (endpoints.size() == 1) {
      // With only one endpoint assume N.. or ..N implying sequence from or to end.
      if (matcher.start() == 0) {
        startValue = 1
        endValue = endpoints[0]
      } else {
        startValue = endpoints[0]
        endValue = max
      }
    } else {
      startValue = endpoints[0]
      endValue = endpoints[1]
    }
  } else {
    startValue = rangeString.isInteger() ? rangeString.toInteger() : null
    endValue = startValue
  }
  logTrace "startValue $startValue, endValue $endValue"
  if (startValue == null || endValue == null) {
    log.warn "Ignoring invalid string $rangeString. It does not define integers."
  } else {
    for (int ii = startValue.toInteger(); ii <= endValue.toInteger(); ii++) {
      result << ii
    }
  }
  return result.findAll { 1 <= it && it <= max }
}

def setStatusLed(String ledString, String colorName, String blinkChoice) {
  logDebug "setStatusLed($ledString, $colorName, $blinkChoice)"
  logDebug "setStatusLed statusLeds $state.statusLeds"
  def cmds = []
  def ledsToUpdate = stringToInts(ledString, numLeds())
  if (!ledsToUpdate) {
    return
  }
  logDebug "ledsToUpdate $ledsToUpdate, size = ${ledsToUpdate.size()}"
  def color = color(colorName)

  if (!state.statusLeds) {
    state.statusLeds = Collections.nCopies(numLeds(), [color: "Off", blink: "No"])
  }
  ledsToUpdate.each {
    state.statusLeds[it - 1] = [color: colorName, blink: blinkChoice]
  }
  logDebug "setStatusLed updated statusLeds $state.statusLeds"

  /*
   * Set led number and color
   */

  if (state.statusLeds.find { !it.color.equals("Off") } == null) {
    // no LEDS are set, put back to NORMAL mode
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: params().ledOperation,
        size: 1).format()
  }
  else {
    // at least one LED is set, put to status mode
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: params().ledOperation,
        size: 1).format()
  }

  ledsToUpdate.each {
      // set color for LEDs
      cmds << zwave.configurationV2.configurationSet(configurationValue: [color.intValue()],
          parameterNumber: params().ledStatusColors[it-1], size: 1).format()
  }

  /*
   * Set blink
   */

  // Update blink mask
  byte blinkMask = 0
  state.statusLeds.eachWithIndex { it, ledIndex ->
    if (it.blink.equals("Yes")) {
        blinkMask |= 0x1 << ledIndex
    }
  }

  logDebug "Setting blink mask $blinkMask ..."
  // Change device blink parameter(s)
  cmds << zwave.configurationV2.configurationSet(configurationValue: [blinkMask],
      parameterNumber: params().ledBlinkMask, size: 1).format()
  // If at least one LED is blinking and blink frequency is not already set, set it to 5 (= a500ms)
  if (blinkMask != 0 &&
      (state.blinkDuration == null | state.blinkDuration < 0 | state.blinkDuration > maxBlinkPeriod())) {
    logDebug "Setting blink default blink period ..."
    state.blinkDuration = defaultBlinkPeriod()
    cmds << zwave.configurationV2.configurationSet(configurationValue: [5],
      parameterNumber: params().ledBlinkPeriod, size: 1).format()
  }

  logTrace "cmds: $cmds"
  delayBetween(cmds, 150)
}

/*
 * Set Dimmer to Normal dimming mode (exit status mode)
 *
 */
def setSwitchModeNormal() {
  logDebug "setSwitchModeNormal()"
  def cmds = []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: params().ledOperation,
      size: 1).format()
  delayBetween(cmds, 500)
}

/*
 * Set Dimmer to Status mode (exit normal mode)
 *
 */
def setSwitchModeStatus() {
  logDebug "setSwitchModeStatus()"
  def cmds = []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: params().ledOperation,
      size: 1).format()
  delayBetween(cmds, 500)
}

/*
 * Set the color of the LEDS for normal dimming mode, shows the current dim level
 */
def setNormalModeLedColor(colorName) {
  logDebug "setNormalModeLedColor($colorName)"
  def cmds = []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [normalColor(colorName)],
      parameterNumber: params().ledNormalColor, size: 1).format()
  logTrace "cmds: $cmds"
  delayBetween(cmds, 500)
}


def poll() {
  logDebug "poll()"
  zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
  logDebug "refresh()"
  configure()
}

def zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd)"
  logTrace "cmd: $cmd"
  logDebug("sceneNumber: ${cmd.sceneNumber} keyAttributes: ${cmd.keyAttributes}")
  def result = []

  // Standard response events for every button push
  if (cmd.keyAttributes != ka().unknown) {
    result += buttonResponse("physical", cmd.sceneNumber, cmd.keyAttributes).collect { createEvent(it) }
  }

  /*
   * Emiller 2019-11-17 We don't have a button for keyAttribute 1. Is it ever triggered?
   * There seems to be some overlap with on/off commands
   * Unknown key attribute 1 was coded to set "on". Why? (I kept logic intact but don't understand it.)
   * Following if:
   *    Key attribute 1 (unknown): Set on or off depending on scene.
   *    Scene 1 (up with key attribute 0 (single tap) or 2 (hold): set on.
   *    Missing here but original code sent off event for scene 2, attribute 2 (hold down)
   *        That seems wrong to me.
   */
  if (cmd.keyAttributes == ka().unknown || (cmd.sceneNumber == scenes().up &&
      (cmd.keyAttributes == ka().tap1 || cmd.keyAttributes == ka().hold))) {
    // Key attribute 1 (unknown): Set on or off depending on scene.
    def switchValue = cmd.sceneNumber == scenes().up ? "on" : "off"
    result = createEvent([name: "switch", value: switchValue, type: "physical"])
  }

  // Set special values per device configuration
  if (singleTapToLevel && cmd.sceneNumber == scenes().up && cmd.keyAttributes == ka().tap1) {
    // Single tap up level
    result += setLevelNoSend(singleTapLevel)
  } else if (cmd.sceneNumber == scenes().up && cmd.keyAttributes == ka().tap2) {
    // Double tap up level
    if (doubleTapUpAction.equals("Set to level")) {
      result += setLevelNoSend(doubleTapUpLevel)
    } else if (doubleTapUpAction.equals("Increase by amount")) {
      result += changeLevelNoSend(doubleTapUpLevel)
    }
  } else if (cmd.sceneNumber == scenes().down && cmd.keyAttributes == ka().tap2) {
    // Double tap down level
    if (doubleTapDownAction.equals("Set to level")) {
      result += setLevelNoSend(doubleTapDownLevel)
    } else if (doubleTapDownAction.equals("Decrease by amount")) {
      result += changeLevelNoSend(-doubleTapDownLevel)
    }
  }

  if (!result) {
    // unexpected case
    log.warn("unexpected scene $cmd.sceneNumber and key attribute $cmd.keyAttribute.")
  }

  return result
}

def buttonResponse(String buttonType, int buttonNum, String direction, int numTaps) {
  logDebug "buttonResponse buttonType ${buttonType}, num ${buttonNum}, dir ${direction}, numTaps ${numTaps}"
  def result = []
  def tapArrow = direction.equals("Down") ? "▼" : "▲"
  def numArrows = numTaps == hold() ? 1 : numTaps
  def tapValue = ""
  for (int ii = 0; ii < numArrows; ii++) {
    tapValue += tapArrow
  }
  def action = numTaps == hold() ? "Hold" : "Tap"
  result << [name: "status", value: "${action} ${tapValue}"]
  result << [name: "pushed", value: buttonNum, descriptionText: "$device.displayName ${action}-${direction}-${numTaps} (button ${buttonNum}) pressed", isStateChange: true]
  logDebug "buttonResponse result ${result}"
  return result

}

def buttonResponse(String buttonType, int scene, int keyAttribute) {
  def button = buttons().find { scene == it.scene && keyAttribute == it.keyAttribute }
  if (button) {
    return buttonResponse(buttonType, button.buttonNum, button.direction, button.numTaps)
  }
}

def buttonResponse(String buttonType, int numTaps, String direction) {
  def button = buttons().find { numTaps == it.numTaps && direction.equals(it.direction) }
  if (button) {
    return buttonResponse(buttonType, button.buttonNum, button.direction, button.numTaps)
  }
}

def tapUp1() {
  buttonResponse("digital", 1, "Up").each { sendEvent(it) }
}

def tapDown1() {
  buttonResponse("digital", 1, "Down").each { sendEvent(it) }
}

def tapUp2() {
  buttonResponse("digital", 2, "Up").each { sendEvent(it) }
}

def tapDown2() {
  buttonResponse("digital", 2, "Down").each { sendEvent(it) }
}

def tapUp3() {
  buttonResponse("digital", 3, "Up").each { sendEvent(it) }
}

def tapDown3() {
  buttonResponse("digital", 3, "Down").each { sendEvent(it) }
}

def tapUp4() {
  buttonResponse("digital", 4, "Up").each { sendEvent(it) }
}

def tapDown4() {
  buttonResponse("digital", 4, "Down").each { sendEvent(it) }
}

def tapUp5() {
  buttonResponse("digital", 5, "Up").each { sendEvent(it) }
}

def tapDown5() {
  buttonResponse("digital", 5, "Down").each { sendEvent(it) }
}

def holdUp() {
  buttonResponse("digital", hold(), "Up").each { sendEvent(it) }
}

def holdDown() {
  buttonResponse("digital", hold(), "Down").each { sendEvent(it) }
}

def configure() {
  logDebug("configure()")
  cleanup()
  sendEvent(name: "numberOfButtons", value: buttons().size(), displayed: false)
  def cmds = []
  cmds += setPrefs()
  cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
  cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  cmds << zwave.versionV1.versionGet().format()
  delayBetween(cmds, 500)
}

def setPrefs() {
  logDebug "setPrefs()"
  def cmds = []

  if (logEnable || traceLogEnable) {
    log.warn "Debug logging is on and will be scheduled to turn off automatically in 30 minutes."
    unschedule()
    runIn(1800, logsOff)
  }

  if (color) {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [normalColor(color)],
      parameterNumber: params().ledNormalColor, size: 1).format()
  }

  if (localcontrolramprate != null) {
    //logDebug localcontrolramprate
    def localRamprate = Math.max(Math.min(localcontrolramprate.toInteger(), 90), 0)
    cmds << zwave.configurationV2.configurationSet(configurationValue: [localRamprate.toInteger()],
        parameterNumber: params().localControlRampRate, size: 1).format()
  }

  if (remotecontrolramprate != null) {
    //logDebug remotecontrolramprate
    def remoteRamprate = Math.max(Math.min(remotecontrolramprate.toInteger(), 90), 0)
    cmds << zwave.configurationV2.configurationSet(configurationValue: [remoteRamprate.toInteger()],
       parameterNumber: params().remoteControlRampRate, size: 1).format()
  }

  if (reverseSwitch) {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: params().loadOrientation,
        size: 1).format()
  }
  else {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: params().loadOrientation,
        size: 1).format()
  }

  if (bottomled) {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: params().ledOff,
        size: 1).format()
  }
  else {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: params().ledOff,
        size: 1).format()
  }

  //Enable the following configuration gets to verify configuration in the logs
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: params().configVerify[0]).format()
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: params().configVerify[1]).format()
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: params().configVerify[2]).format()
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: params().configVerify[3]).format()

  logTrace "cmds: $cmds"
  return cmds
}

def logsOff() {
  log.info "Turning off debug logging for device ${device.label}"
  device.updateSetting("logEnable", [value: "false", type: "bool"])
  device.updateSetting("traceLogEnable", [value: "false", type: "bool"])
}

def updated() {
  logDebug "updated()"
  def cmds = []
  cmds += setPrefs()
  delayBetween(cmds, 500)
}

void installed() {
  logDebug "installed()"
  cleanup()
}

void cleanup() {
  logDebug "cleanup()"
  unschedule()
  state.clear()
  state.statusLeds = Collections.nCopies(numLeds(), [color: "Off", blink: "No"])
}

void logInfo(GString msg) {
  logInfo msg.toString()
}

void logInfo(String msg) {
  if (descriptionTextEnable) log.info msg
}

void logDebug(GString msg) {
  logDebug msg.toString()
}

void logDebug(String msg) {
  if (logEnable) log.debug msg
}

void logTrace(GString msg) {
  logTrace msg.toString()
}

void logTrace(String msg) {
  if (traceLogEnable) log.trace msg
}
