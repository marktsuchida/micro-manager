project "MMDevice"
   basedir "."
   kind "StaticLib"

   configuration { "vs*" }
      defines {
         "MODULE_EXPORTS"
      }

   files { "**.h", "**.cpp" }
   excludes { "unittest/**" }


project "MMDevice-StaticRuntime"
   basedir "."
   kind "StaticLib"

   flags {
      "StaticRuntime"
   }

   configuration { "vs*" }
      defines {
         "MODULE_EXPORTS"
      }

   files { "**.h", "**.cpp" }
   excludes { "unittest/**" }


-- TODO MMDevice-Tests project
