# WisperingTree
Software for a public audio installation called "Whispdom"

![How "Whispdom" works](https://www.dropbox.com/s/tu1kb14bp7z2qb8/whispdom-sketch2-367px.jpg?dl=1)

## How it basically works
The app consists of a client and a server implementation. One server is recording sounds by detecting peak levels in the audio stream. 
The stored files are sent through bluetooth to all clients which registered to the server.
The clients play the audio files in a sequence.
