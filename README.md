# zk9500 Fingerprint Scanner Background App

## Overview
The zk9500 Fingerprint Scanner Background App is a Java-based application that runs as a background service, capturing fingerprint data and communicating with local applications via WebSockets.

## Installation & Usage
1. Ensure you have Java 21 or later installed on your system.
2. Download or clone the project.
3. Open a terminal or command prompt and navigate to the project directory.
4. Compile and run the main class with the following command:
   ```sh
   java zk9500scanner.Main --wsport=51515
   ```
   Replace `zk9500scanner.Main` with the actual main class path and `51515` with your desired WebSocket port if needed.

## Features
- Runs as a background service
- Captures fingerprint data in real time
- Communicates via WebSockets
- Configurable WebSocket port
