MyClient - Fabric 1.21.4

BUDOWANIE
1. Potrzebujesz JDK 21.
2. Brakuje gradle wrappera (gradlew) - skopiuj pliki gradlew, gradlew.bat oraz folder gradle/
   z oficjalnego szablonu: https://github.com/FabricMC/fabric-example-mod (Gradle 8.12),
   ALBO jesli masz zainstalowanego Gradle:  gradle wrapper --gradle-version 8.12
3. W folderze projektu:  ./gradlew build   (Windows: gradlew.bat build)
4. Gotowy mod: build/libs/myclient-1.0.0.jar -> wrzuc do .minecraft/mods
   (potrzebny tez Fabric Loader 0.16+ i Fabric API dla 1.21.4)

UZYCIE
Right Shift = menu. LPM na module = wlacz/wylacz, PPM = ustawienia.
Config zapisuje sie w .minecraft/config/myclient.json

DODAWANIE MODULU
Stworz klase dziedziczaca po Module i dodaj ja w ModuleManager.init().
