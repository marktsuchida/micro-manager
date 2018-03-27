deviceadapter "DECamera"
   links {
      "DEMessaging",
      "DEClientLib"
   }

   files { "DECamera/*.h", "DECamera/*.cpp" }


project "DEClientLib"
   kind "StaticLib"

   files { "DEClientLib/*.h", "DEClientLib/*.cpp" }


project "DEMessaging"
   kind "StaticLib"

   files {
      "DEMessaging/DEServer.proto",
      "DEMessaging/DEServer.pb.h",
      "DEMessaging/DEServer.pb.cc",
      "DEMessaging/*.h",
      "DEMessaging/*.cpp",
   }

   configuration { "windows" }
      custombuildtask {
         {
            "DEMessaging/DEServer.proto",
            "DEMessaging/DEServer.pb.cc",
            {},
            {
               "protoc --proto_path=DEMessaging --cpp_out=DEMessaging $(<)",
            }
         },
      }
