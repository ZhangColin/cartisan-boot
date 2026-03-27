rootProject.name = "cartisan-boot"

plugins {
    id("info.solidsoft.pitest") version "1.19.0-rc.3" apply false
}

include("cartisan-dependencies")
include("cartisan-core")
include("cartisan-test")
include("cartisan-web")
include("cartisan-data-jpa")
include("cartisan-event")
include("cartisan-security")
include("cartisan-data-query")
include("cartisan-ai")
