project "MMCore"
   basedir "."
   kind "StaticLib"

   links {
      "MMDevice",
   }

   files { "**.h", "**.cpp" }
   -- TODO Cleaner if we put sources under 'src/' but now is not a good time to
   -- move sources
   excludes { "unittest/**.cpp" }

   configuration { "vs*" }
      -- Disable warning C4290 "C++ exception specification ignored except to
      -- indicate a function is not __declspec(nothrow)"
      buildoptions_cpp { "/wd4290" }


-- TODO MMCore-Tests project
