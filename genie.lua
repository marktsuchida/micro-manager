-- Copyright (C) 2018 Open Imaging, Inc.
-- BSD-3

-- Visual Studio project file generation for Micro-Manager
-- See: https://github.com/bkaradzic/GENie
--
-- Hint: Indentation is not significant in Lua code (unlike in Python). GENie
-- scripts are indented only to show logical structure. Commands like
-- "solution", "project", or "configuration" work by changing the "current"
-- solution or project or configuration to modify.

require "geniescripts.jni"
require "geniescripts.python"
require "geniescripts.protobuf"


-- Use Windows7.1SDK instead of vc100. TODO Once we switch to newer VS, it
-- might be useful to add a cli option (--toolset) to set toolset.
if _ACTION == "vs2010" then
   premake.vstudio.toolset = "Windows7.1SDK"
end


solution "micro-manager"
   language "C++"

   basedir "."
   objdir "build"

   configurations {
      "Debug",
      "Release",
   }

   platforms {
      "x32",
      "x64",
   }

   -- -- --

   flags {
      "ExtraWarnings",
   }

   configuration { "Debug" }
      flags {
         "Symbols",
         "FullSymbols",
         "EnableMinimalRebuild",
      }

   configuration { "Release" }
      flags {
         "Symbols",
         "OptimizeSpeed",
         "NoIncrementalLink",
      }

   configuration { "windows" }
      defines {
         "WIN32",
         "_WINDOWS",
      }

   configuration { "Debug" }
      defines { "DEBUG" }

   configuration { "Release" }
      defines { "NDEBUG" }

   configuration { "vs*", "Debug" }
      defines { "_DEBUG" }

   configuration { "vs*" }
      buildoptions_cpp {
         -- Disable warning C4127 "conditional expression is constant" because
         -- constant conditional values are perfectly fine and useful
         -- TODO In C++ 11+ perhaps we should prefer constexpr
         "/wd4127"
      }

      defines {
         "_CRT_SECURE_NO_WARNINGS",
         "_SCL_SECURE_NO_WARNINGS",
      }

   -- -- --

   startproject "MMCore"

   group "Micro-Manager"
      include "MMDevice"
      include "MMCore"

      if JAVA_HOME then
         include "MMCoreJ_wrap"
      else
         print("Skipping MMCoreJ_wrap")
      end

      if PYTHON_INCLUDE_DIR and NUMPY_INCLUDE_DIRS then
         include "MMCorePy_wrap"
      else
         print("Skipping MMCorePy_wrap")
      end

   group "DeviceAdapters"
      include "DeviceAdapters"
