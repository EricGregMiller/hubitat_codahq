/**
 *  MIMOlite device type for garage door button, including power failure indicator.  Be sure mimolite has jumper removed before
 *  including the device to your hub, and tap Config to ensure power alarm is subscribed.
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
 *  Updates:
 *  -------
 *  2019-12-12 : emiller copied from https://community.hubitat.com/t/current-recommend-garage-door-opener-closer/4686/2
 *  02-18-2016 : Initial commit
 *  03-05-2016 : Changed date format to MM-dd-yyyy h:mm a
 *  03-11-2016 : Due to ST's v2.1.0 app totally hosing up SECONDARY_CONTROL, implemented a workaround to display that info in a separate tile.
 *  08-27-2016 : Modified the device handler for my liking, primarly for looks and feel.
 *  01-08/2017 : Added code for Health Check capabilities/functions.
 *  02-11-2017 : Cleaned up code, and used secondary_control again for messages.
 *  03-11-2017 : Cleaned up code.
 *  03-24-2017 : Changed color schema to match ST's new format.
 *  04-08-2017 : Updated the updated() section to call configuration().
 *  05-19-2017 : Added additional attributeStates to match ST's DTH which should make this work with ActionTiles, and to use contact as the main tile instead of switch due to personal preference.
 *  05-20-2017 : Redefined tiles/names to be similar to the Linear-type opener DTH's, which should make this work with ActionTiles.
 *  05-23-2017 : Made the delay longer for retreiving device info (gets) after the main tile or the refresh tile is tapped.
 *  09-22-2019 : converted DH to Hubitat and added refresh before each action
 *  10-07-2019 : removed extra refresh from open/close it caused to much delay
 */
metadata {
    // Automatically generated. Make future change here.
    definition (name: "MIMOlite Garage Door Controller 10/07/2019", namespace: "Hubitat", author: "scgs350") {
        capability "Momentary"
        capability "Relay Switch"
        capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Sensor"
        capability "Contact Sensor"
        capability "Configuration"
        capability "Actuator"
        capability "Door Control"
        capability "Garage Door Control"
        capability "Health Check"

        attribute "powered", "string"
        attribute "contactState", "string"

        command "on"
        command "off"
        command "open"
        command "close"
    }

    preferences {
        input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
    }


}

def updated(){
    // Device-Watch simply pings if no device events received for 32min(checkInterval)
    sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    response(configure())
}

def parse(String description) {
    log.debug "description is: ${description}"
    def result = null
    def cmd = zwave.parse(description, [0x72: 1, 0x86: 1, 0x71: 1, 0x30: 1, 0x31: 3, 0x35: 1, 0x70: 1, 0x85: 1, 0x25: 1, 0x03: 1, 0x20: 1, 0x84: 1])
    log.debug "command value is: $cmd.CMD"
    if (cmd.CMD == "7105") {				//Mimo sent a power loss report
        log.debug "Device lost power"
        sendEvent(name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power")
    } else {
        sendEvent(name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power")
    }
    if (cmd) {
        result = createEvent(zwaveEvent(cmd))
    }
    log.debug "Parse returned ${result?.descriptionText}"
    def statusTextmsg = ""
    def timeString = new Date().format("MM-dd-yy h:mm a", location.timeZone)
    statusTextmsg = "Last updated: "+timeString
    sendEvent("name":"statusText", "value":statusTextmsg)
    return result
}

def sensorValueEvent(Short value) {
    if (value) {
        sendEvent(name: "contact", value: "open")
        sendEvent(name: "door", value: "open")
        sendEvent(name: "switch", value: "on")
        sendEvent(name: "contactState", value: "Door is open, close/off to close")
    } else {
        sendEvent(name: "contact", value: "closed")
        sendEvent(name: "door", value: "closed")
        sendEvent(name: "switch", value: "off")
        sendEvent(name: "contactState", value: "Door is closed, open/on to open")
    }
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    [name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    def doorState = device.currentValue('switch')
    if (doorState == "off")
        [name: "contactState", value: cmd.value ? "close" : "closing", type: "digital"]
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd)
{
    sensorValueEvent(cmd.value)
}

def zwaveEvent(hubitat.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
    sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(hubitat.zwave.commands.alarmv1.AlarmReport cmd)
{
    log.debug "We lost power" //we caught this up in the parse method. This method not used.
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    // Handles all Z-Wave commands we aren't interested in
    [:]
}

def on() {
    log.debug "Sending on event to open door"
    open()
}

def off() {
    log.debug "Sending off event to close door"
    close()
}

def open() {
    if (device.currentValue("door") != "closed") {
        log.debug "not opening door is already open"
    }
    else {
        log.debug "Sending open event to open door"
        push()
    }
}

def close() {
    if (device.currentValue("door") != "open") {
        log.debug "Not closing door since it is already closed"
    }
    else {
        log.debug "Sending close event to close door"
        push()
    }
}

def push() {
    log.debug "Executing push for garage door"
    delayBetween([
            zwave.basicV1.basicGet().format(),
            zwave.basicV1.basicSet(value: 0xFF).format(),
    ],1000)
}

def poll() {
    refresh()
}

// PING is used by Device-Watch in attempt to reach the Device
def ping() {
    refresh()
}

def refresh() {
    log.debug "Refreshing"
    delayBetween([
            zwave.switchBinaryV1.switchBinaryGet().format(),
            zwave.sensorBinaryV1.sensorBinaryGet().format(),
            zwave.basicV1.basicGet().format(),
//		zwave.alarmV1.alarmGet(alarmType: 0x08).format()
    ],1000)
}

def configure() {
    log.debug "Configuring...." //setting up to monitor power alarm and actuator duration
    delayBetween([
            zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(),
            zwave.configurationV1.configurationSet(parameterNumber: 11, configurationValue: [25], size: 1).format(),
            zwave.configurationV1.configurationGet(parameterNumber: 11).format()
    ],100)
}

def cleanup() {
    logDebug "cleanup()"
    unschedule()
    state.clear()
    state.statusLeds = Collections.nCopies(numLeds(), [color: "Off", blink: "No"])
}

private logInfo(msg) {
    if (descriptionTextEnable) log.info msg
}

def logDebug(msg) {
    if (logEnable) log.debug msg
}

def logTrace(msg) {
    if (traceLogEnable) log.trace msg
}
