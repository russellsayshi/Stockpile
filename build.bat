@echo off
if not exist build\icon.png (
xcopy icon.png build
)
javac -d build src\com\russellsayshi\stockpile\server\*.java src\com\russellsayshi\stockpile\inventory\*.java src\com\russellsayshi\stockpile\gui\*.java