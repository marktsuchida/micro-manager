project "MMDevice"
   basedir "."
   kind "StaticLib"

   files { "**.h", "**.cpp" }
   excludes { "unittest/**" }


project "MMDevice-StaticRuntime"
   basedir "."
   kind "StaticLib"

   files { "**.h", "**.cpp" }
   excludes { "unittest/**" }

   flags {
      "StaticRuntime"
   }


-- TODO MMDevice-Tests project
