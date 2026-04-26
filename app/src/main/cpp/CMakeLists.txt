# using sqlite-amalgamation-3530000
# get NativeDB.c from xerial sqlite-jdbc src/main/java/org/sqlite/core

cmake_minimum_required(VERSION 3.22)

project("sqlite_jdbc_native")

add_library(sqlitejdbc SHARED
            NativeDB.c
            sqlite3.c)

# Enable the metadata functions that JDBC needs
target_compile_definitions(sqlitejdbc PRIVATE
                           SQLITE_ENABLE_COLUMN_METADATA
                           SQLITE_ENABLE_FTS5
                           SQLITE_ENABLE_RTREE
                           SQLITE_ENABLE_JSON1)

target_compile_options(sqlitejdbc PRIVATE -Wno-incompatible-pointer-types)

# We don't need JNI_INCLUDE_DIRS because the NDK provides them automatically.
target_include_directories(sqlitejdbc PRIVATE
                           ${CMAKE_CURRENT_SOURCE_DIR})

# Link the Android log library
target_link_libraries(sqlitejdbc log)