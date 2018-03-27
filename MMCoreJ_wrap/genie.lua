project "MMCoreJ_wrap"
   basedir "."
   kind "Bundle"

   links {
      "MMCore"
   }

   flags {
      "NoImportLib",
   }

   configuration { "vs*" }
      -- Disable warning C4290 "C++ exception specification ignored except to
      -- indicate a function is not __declspec(nothrow)"
      buildoptions_cpp { "/wd4290" }

      defines {
         "MMCOREJ_WRAP_EXPORTS",
      }

   configuration {}
      includedirs {
         JAVA_HOME .. "/include",
      }

   configuration { "windows" }
      includedirs {
         JAVA_HOME .. "/include/win32",
      }

   configuration { "windows" }
      custombuildtask {
         {
            "MMCoreJ.i",
            "gensrc/jni/MMCoreJ_wrap.cxx",
            {},
            {
               -- Make sure we don't mix existing and new Java sources
               "@del /s/q mmcorej/src/main/java/mmcorej > NUL",
               "@mkdir mmcorej/src/main/java/mmcorej > NUL",
               "swig -c++ -java -package mmcorej -module MMCoreJ " ..
                  "-outdir mmcorej/src/main/java/mmcorej -o $(@) $(<)",
            }
         },
      }

   files {
      "gensrc/jni/MMCoreJ_wrap.h",
      "gensrc/jni/MMCoreJ_wrap.cxx",
   }
