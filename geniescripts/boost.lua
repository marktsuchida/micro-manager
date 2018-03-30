-- Copyright (C) 2018 Open Imaging, Inc.
-- BSD-3

newoption {
   trigger = "boost-libdir",
   value = "DIR",
   description = "Boost libraries",
}
local boost_libdir_flag = _OPTIONS["boost-libdir"]

newoption {
   trigger = "boost-include",
   value = "DIR",
   description = "Boost headers",
}
local boost_include_flag = _OPTIONS["boost-include"]


-- TODO Check if values look correct
-- TODO Add guess of location for Posix
BOOST_LIBRARY_DIR = boost_libdir_flag
BOOST_INCLUDE_DIR = boost_include_flag
