-- Copyright (C) 2018 Open Imaging, Inc.
-- BSD-3

newoption {
   trigger = "protoc",
   value = "EXECUTABLE",
   description = "Protocol Buffers Compiler executable (default: protoc on PATH)",
}
local protoc_flag = _OPTIONS["protoc"]

newoption {
   trigger = "protobuf-libdir",
   value = "DIR",
   description = "Protocol Buffers libraries",
}
local protobuf_libdir_flag = _OPTIONS["protobuf-libdir"]

newoption {
   trigger = "protobuf-include",
   value = "DIR",
   description = "Protocol Buffers headers",
}
local protobuf_include_flag = _OPTIONS["protobuf-include"]


local protoc_cmd = nil
print("Checking for protoc...")
if protoc_flag then
   if os.execute(protoc_flag .. " --version") then
      protoc_cmd = protoc_flag
   else
      print("protoc doesn't seem to work: " .. protoc_flag)
   end
elseif os.execute("protoc --version") then
   protoc_cmd = "protoc"
end

if protoc_cmd then
   PROTOC = protoc_cmd
end

if not PROTOC or string.len(PROTOC) == 0 then
   print("protoc not found")
end


-- TODO Check if values look correct
-- TODO Add guess of location from protoc path
PROTOBUF_LIBRARY_DIR = protobuf_libdir_flag
PROTOBUF_INCLUDE_DIR = protobuf_include_flag
