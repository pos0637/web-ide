import { Mock } from '~/components/request';
import { getRandom } from '~/misc/random';

Mock.onGet(/api\/v1\/board\/overview[?.*]?/).reply(() =>
    [200, {
        code: 200,
        data: {
            runningInformation: getRandom(0, 1)
        }
    }]
);

Mock.onGet(/api\/v1\/board\/main[?.*]?/).reply(() =>
    [200, {
        code: 200,
        data: {
            gridConnectedCabinetChargingElectricity: getRandom(0, 1600),
            gridConnectedCabinetDischargingElectricity: getRandom(0, 1600),
            gridConnectedCabinetPower: getRandom(0, 220),
            gridConnectedCabinetPowerData: {
                xLabels: ['0:00', '3:00', '6:00', '9:00', '12:00', '15:00', '18:00', '21:00', '23:00'],
                data: [getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220)]
            },
            switch1: getRandom(0, 1),
            switch2: getRandom(0, 1),
            pcsPower: getRandom(0, 200),
            pcsVoltage1: getRandom(0, 220),
            pcsCurrent: getRandom(0, 200),
            pcsVoltage2: getRandom(0, 220),
            pcsChargingElectricity: getRandom(0, 1600),
            pcsDischargingElectricity: getRandom(0, 1600)
        }
    }]
);

Mock.onGet(/api\/v1\/board\/load[?.*]?/).reply(() =>
    [200, {
        code: 200,
        data: {
            power1: getRandom(0, 200),
            power1Data: {
                xLabels: ['0:00', '3:00', '6:00', '9:00', '12:00', '15:00', '18:00', '21:00', '23:00'],
                data: [getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220)]
            },
            power2: getRandom(0, 200),
            power2Data: {
                xLabels: ['0:00', '3:00', '6:00', '9:00', '12:00', '15:00', '18:00', '21:00', '23:00'],
                data: [getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220)]
            }
        }
    }]
);

Mock.onGet(/api\/v1\/board\/transformer[?.*]?/).reply(() =>
    [200, {
        code: 200,
        data: {
            chargingElectricity1: getRandom(0, 200),
            dischargingElectricity1: getRandom(0, 200),
            power1: getRandom(0, 200),
            power1Data: {
                xLabels: ['0:00', '3:00', '6:00', '9:00', '12:00', '15:00', '18:00', '21:00', '23:00'],
                data: [getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220), getRandom(0, 220)]
            }
        }
    }]
);

Mock.onGet(/api\/v1\/board\/bms[?.*]?/).reply(() =>
    [200, {
        code: 200,
        data: {
            soc1: getRandom(0, 200),
            voltage1: getRandom(0, 200),
            soc2: getRandom(0, 200),
            voltage2: getRandom(0, 200)
        }
    }]
);
