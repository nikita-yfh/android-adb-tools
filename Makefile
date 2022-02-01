all: debug
debug:
	./gradlew installDebug
release:
	./gradlew assembleRelease
	apksigner sign --ks ../nikita.jks --out app/release.apk app/build/outputs/apk/app-release-unsigned.apk
clean:
	./gradlew clean
