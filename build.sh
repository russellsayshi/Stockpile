#!/bin/bash
cd src
javac -d ../build com/russellsayshi/stockpile/*/*.java
cd ..
if [ ! -f build/icon.png ]; then
	cp icon.png build
fi
