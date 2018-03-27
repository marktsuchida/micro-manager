project "MMCorePy_wrap"
   basedir "."
   kind "Bundle"

   links {
      "MMCore"
   }

   flags {
      "NoImportLib",
   }

   targetname "_MMCorePy"
   targetextension ".pyd"

   configuration { "vs*" }
      -- Disable warning C4290 "C++ exception specification ignored except to
      -- indicate a function is not __declspec(nothrow)"
      buildoptions_cpp { "/wd4290" }

      defines {
         "__WIN32__",
         "MMCOREPY_WRAP_EXPORTS",
      }

   configuration {}
      includedirs {
         PYTHON_INCLUDE_DIR,
      }
      for i, d in ipairs(NUMPY_INCLUDE_DIRS) do
         includedirs { d }
      end

   configuration { "windows" }
      custombuildtask {
         {
            "MMCorePy.i",
            "gensrc/MMCorePy_wrap.cxx",
            {},
            {
               "@mkdir gensrc > NUL",
               "swig -c++ -python -outdir gensrc -o $(@) $(<)",
            }
         },
      }

   files {
      "gensrc/MMCorePy_wrap.h",
      "gensrc/MMCorePy_wrap.cxx",
   }
