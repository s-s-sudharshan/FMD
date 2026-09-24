package com.infy.simulator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.infy.entity.Device;
import com.infy.enums.DeviceState;
import com.infy.enums.Severity;
import com.infy.enums.TrapType;
import com.infy.repository.DeviceRepository;

/** Builds a random batch of alarms, as XML, for the currently ACTIVATED devices (BE US16). */
@Component
public class AlarmXmlGenerator {

    @Autowired
    private DeviceRepository deviceRepository;

    @Value("${app.simulator.max-alarms-per-run:3}")
    private int maxAlarmsPerRun;

    /** @return the XML, or null when there are no active devices to raise alarms for. */
    public String generate() {
        List<Device> devices = deviceRepository.findAllByDeviceState(DeviceState.ACTIVATED);
        if (devices.isEmpty()) {
            return null;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Severity[] severities = Severity.values();
        TrapType[] traps = TrapType.values();
        int count = rnd.nextInt(1, Math.max(1, maxAlarmsPerRun) + 1);

        // Values are enums / validated IPv4 strings, so no XML escaping is needed.
        StringBuilder xml = new StringBuilder("<alarms>");
        for (int i = 0; i < count; i++) {
            Device d = devices.get(rnd.nextInt(devices.size()));
            xml.append("<alarm>")
               .append("<deviceIp>").append(d.getIpAddress()).append("</deviceIp>")
               .append("<severity>").append(severities[rnd.nextInt(severities.length)]).append("</severity>")
               .append("<trap>").append(traps[rnd.nextInt(traps.length)]).append("</trap>")
               .append("</alarm>");
        }
        return xml.append("</alarms>").toString();
    }
}
