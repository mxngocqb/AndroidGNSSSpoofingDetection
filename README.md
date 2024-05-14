# GNSS Spoofing Detection for Mobile devices
This is my research project in the field of GNSS. This project provides accurate PVT data to the end user.
## Principle
The proposed solution combines the advantages of both methods. The system will be divided into two parts:

First of all, part of the system will focus on identifying fake signals. If a signal is determined to be authentic, the system will use the digital signature to generate a signature for the NAV.

Next, the receiver will receive navigation data from the supply system, authenticate it with a digital signature and then compare it with the navigation data it received. If any discrepancies are detected, the instrument removes the corresponding satellite from the Position, Velocity and Time (PVT) calculation. As a result, the device will be able to eliminate the influence of spurious signals on the PVT.

<img src="https://github.com/mxngocqb/AndroidGNSSSpoofingDetection/blob/master/picture/solution.jpg" alt="Solution image" width="900" />

You can access the server that provide accurate Navigation Message at the following URL: [ace-rationally-flounder.ngrok-free.app](https://ace-rationally-flounder.ngrok-free.app)
For detailed , please visit the following link: [GNSS Spoofing Detection Backend](https://github.com/mxngocqb/RSA)
## User manual
For implementing GNSS spoofing detection, download the pseudorange module from [here](https://github.com/mxngocqb/AndroidGNSSpoofingDetection/tree/master/pseudorange) and the essential files: `MeasurementListener`, `MeasurementProvider`, and `RealtimePositionsVelocityCalculator` like my example project [here](https://github.com/mxngocqb/AndroidGNSSpoofingDetection/tree/master/app).



