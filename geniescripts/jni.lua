-- Copyright (C) 2018 Open Imaging, Inc.
-- BSD-3

newoption {
   trigger = "java-home",
   value = "PATH",
   description = "JDK home directory (overrides the JAVA_HOME environment variable)",
}
local java_home_flag = _OPTIONS["java-home"]


JAVA_HOME = os.getenv("JAVA_HOME")
if java_home_flag then
   JAVA_HOME = java_home_flag
end

if not JAVA_HOME or string.len(JAVA_HOME) == 0 then
   print("JDK location not set")
else
   print("Using JDK at " .. JAVA_HOME)
   local jni_h = JAVA_HOME .. "/include/jni.h"
   if not os.isfile(jni_h) then
      print("Cannot find " .. jni_h)
      print("Make sure you have a JDK, not JRE")
      JAVA_HOME = nil
   end
end

if not JAVA_HOME then
   print("Not generating projects requiring Java")
end
