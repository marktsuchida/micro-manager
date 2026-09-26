#!/usr/bin/env bash
# ^^^ We need Homebrew bash, not the ancient one bundled with macOS.

set -e

usage() {
   echo "Usage: $0 -a ARCH -d DIR" 1>&2
   echo "   -a ARCH -- thin universal binaries, keeping only ARCH binaries" 1>&2
   echo "   -d DIR  -- thin universal binaries in DIR, recursively" 1>&2
   echo "Unpacked JAR directories (*.jar) under DIR that contain native code but" 1>&2
   echo "none for ARCH are removed." 1>&2
   echo "Both flags are required." 1>&2
   exit 1
}

keep_arch=
thin_dir=
while getopts ":a:d:" o; do
   case $o in
      a) keep_arch="$OPTARG" ;;
      d) thin_dir="$OPTARG" ;;
      *) usage ;;
   esac
done

[ -z "$keep_arch" ] && usage
[ -z "$thin_dir" ] && usage


has_arch() {
   lipo -archs "$1" | tr ' ' '\n' | grep -qx "$keep_arch"
}


# A JAR may carry separate single-arch libraries (e.g. x86_64 and arm64), so
# only remove it if none of its Mach-O files has the arch.
remove_jar_if_no_arch() {
   jar="$1"
   found_macho=no
   readarray -d '' -t jar_files < <(find "$jar" -type f -print0)
   for file in "${jar_files[@]}"; do
      if file -b "$file" | grep -q 'Mach-O'; then
         found_macho=yes
         has_arch "$file" && return
      fi
   done
   if [ "$found_macho" = yes ]; then
      echo "Warning: removing $jar, which has no $keep_arch native code" 1>&2
      rm -rf "$jar"
   fi
}


thin_binary() {
   file="$1"
   if ! has_arch "$file"; then
      echo "Warning: $file has no $keep_arch slice; leaving as is" 1>&2
      return
   fi
   echo "Keeping only $keep_arch from $file"
   lipo -extract_family $keep_arch "$file" -output "$file.thin"
   mv "$file.thin" "$file"
}


readarray -d '' -t jars < <(find "$thin_dir" -type d -name '*.jar' -print0)
for jar in "${jars[@]}"; do
   remove_jar_if_no_arch "$jar"
done

readarray -d '' -t files < <(find "$thin_dir" -type f -print0)
for file in "${files[@]}"; do
   if file "$file" | grep -q 'Mach-O universal binary'; then
      thin_binary "$file"
   fi
done
