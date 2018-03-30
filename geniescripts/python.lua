-- Copyright (C) 2018 Open Imaging, Inc.
-- BSD-3

newoption {
   trigger = "python",
   value = "EXECUTABLE",
   description = "Python executable (overrides python found on PATH)",
}
local python_flag = _OPTIONS["python"]


local python_cmd = nil
print("Checking for python...")
if python_flag then
   if os.execute(python_flag .. " --version") then
      python_cmd = python_flag
   else
      print("python doesn't seem to work: " .. python_flag)
   end
elseif os.execute("python --version") then
   python_cmd = "python"
elseif os.execute("python3 --version") then
   python_cmd = "python3"
elseif os.execute("python2 --version") then
   python_cmd = "python2"
end

if python_cmd then
   PYTHON = python_cmd
end

if not PYTHON or string.len(PYTHON) == 0 then
   print("Python executable not found")
else
   print("Using Python: " .. PYTHON)

   PYTHON_INCLUDE_DIR = os.outputof(PYTHON ..
         ' -c "import distutils.sysconfig as c, sys;' ..
         ' sys.stdout.write(c.get_python_inc(True))"')
   if PYTHON_INCLUDE_DIR then
      print("Using Python headers at " .. PYTHON_INCLUDE_DIR)
   else
      print("Cannot determine Python include dir")
   end

   print("Checking for NumPy...")
   if not os.execute(PYTHON .. ' -c "import numpy"') then
      print("NumPy is not available")
      print("Not generating projects requiring NumPy")
   else
      local numpy_inc_dirs = os.outputof(PYTHON ..
         ' -c "from __future__ import print_function;' ..
         ' import numpy.distutils.misc_util as c;' ..
         ' [print(d) for d in c.get_numpy_include_dirs()]"')
      if not numpy_inc_dirs then
         print("Cannot determine NumPy include dirs")
      end
      NUMPY_INCLUDE_DIRS = {}
      for d in string.gmatch(numpy_inc_dirs, "([^\n]+)") do
         if string.len(d) > 0 then
            table.insert(NUMPY_INCLUDE_DIRS, d)
         end
      end
      print("Using NumPy headers at " .. table.concat(NUMPY_INCLUDE_DIRS, ", "))
   end
end
if not PYTHON then
   print("Not generating projects requiring Python")
end
