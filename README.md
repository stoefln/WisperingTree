# WisperingTree
Software for a public audio installation called "Whispdom"

![How "Whispdom" works](https://dl.dropboxusercontent.com/u/8343806/whispdom-sketch2-367px.jpg)

## How it basically works
The app consists of a client and a server implementation. One server is recording sounds by detecting peak levels in the audio stream. 
Each recorded audio file is sent through bluetooth to one of the clients which registered at the server.
The clients play the audio files in a sequence.

## Licence

WisperingTree is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

WisperingTree is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

For a copy of the GNU General Public License see <http://www.gnu.org/licenses/>.
