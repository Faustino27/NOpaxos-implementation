# Build the project
Write-Host "Building the project..."
mvn clean install

# Check if the build was successful
if ($LastExitCode -ne 0) {
    Write-Host "Build failed, exiting script."
    exit
}

# Start the replicas
Write-Host "Starting replicas..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "java -cp target/paxos-implementation-0.1.0-SNAPSHOT.jar models.Replica 9001"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "java -cp target/paxos-implementation-0.1.0-SNAPSHOT.jar models.Replica 9002"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "java -cp target/paxos-implementation-0.1.0-SNAPSHOT.jar models.Replica 9003"


# Wait a bit for replicas to initialize
Start-Sleep -Seconds 2

# Start the sequencer
Write-Host "Starting the sequencer..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "java -cp target/paxos-implementation-0.1.0-SNAPSHOT.jar models.Sequencer"


# Wait a bit for the sequencer to initialize
Start-Sleep -Seconds 2

# Start a single client
Write-Host "Starting a single client..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "java -cp target/paxos-implementation-0.1.0-SNAPSHOT.jar models.Client 'localhost' 8080"

# Display running jobs
Get-Job | Receive-Job -Wait -AutoRemoveJob
