package net.forestany.mediacollection.main

import android.content.Context
import android.util.Log
import net.forestany.mediacollection.R
import java.util.Properties

class Database(dbPath: String, cachePath: String, context: Context) {

    companion object {
        private const val TAG = "Database"
    }

    private val glob: GlobalInstance = GlobalInstance.get()
    private val tableName: String = "mediacollection"
    private val tableNameLanguages: String = "languages"

    init {
        var b_foo = false

        // check if database not exists
        if (!net.forestany.forestj.lib.io.File.exists(dbPath)) {
            b_foo = true
        }

        // create sqlite db instance
        glob.fJGlob.BaseGateway = net.forestany.forestj.lib.sqlcore.BaseGateway.SQLITE
        glob.fJGlob.Base = net.forestany.forestj.lib.sql.sqlite.BaseSQLite(dbPath)
        glob.fJGlob.logCompleteSqlQuery = false

        // database missing, must initialize it
        if (b_foo) {
            initializeDatabase()
        }

        // test connection to sqlite database
        if (!glob.fJGlob.Base.testConnection()) {
            throw Exception(context.getString(R.string.main_no_db))
        }

        // avoiding SQLITE_IOERR_GETTEMPPATH with this PRAGMA
        val o_queryAny = net.forestany.forestj.lib.sql.Query<net.forestany.forestj.lib.sql.Select>(glob.fJGlob.BaseGateway, net.forestany.forestj.lib.sqlcore.SqlType.ALTER, tableName)
        o_queryAny.setQuery("PRAGMA temp_store_directory = '$cachePath'")
        glob.fJGlob.Base.fetchQuery(o_queryAny)
    }

    private fun initializeDatabase() {
        /* #### CREATE ############################################################################# */
        var o_queryCreate = net.forestany.forestj.lib.sql.Query<net.forestany.forestj.lib.sql.Create>(glob.fJGlob.BaseGateway, net.forestany.forestj.lib.sqlcore.SqlType.CREATE, tableName)

        /* #### Columns ############################################################################ */
        var a_columnsDefinition: MutableList<Properties> = ArrayList()
        var o_properties = Properties()

        o_properties["name"] = "Id"
        o_properties["columnType"] = "integer [int]"
        o_properties["constraints"] = "NOT NULL;PRIMARY KEY;AUTO_INCREMENT"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "UUID"
        o_properties["columnType"] = "text [36]"
        o_properties["constraints"] = "NOT NULL;UNIQUE"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Title"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NOT NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Type"
        o_properties["columnType"] = "text [36]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "PublicationYear"
        o_properties["columnType"] = "integer [small]"
        o_properties["constraints"] = "NOT NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "OriginalTitle"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NOT NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "SubType"
        o_properties["columnType"] = "text [36]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "FiledUnder"
        o_properties["columnType"] = "text [36]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "LastSeen"
        o_properties["columnType"] = "datetime"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "LengthInMinutes"
        o_properties["columnType"] = "integer [small]"
        o_properties["constraints"] = "NOT NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Languages"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Subtitles"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Directors"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Screenwriters"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Cast"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "SpecialFeatures"
        o_properties["columnType"] = "text"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Other"
        o_properties["columnType"] = "text"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "LastModified"
        o_properties["columnType"] = "datetime"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Deleted"
        o_properties["columnType"] = "datetime"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Poster"
        o_properties["columnType"] = "text"
        o_properties["constraints"] = "NULL"
        a_columnsDefinition.add(o_properties)

        /* #### Query ############################################################################ */
        for (o_columnDefinition in a_columnsDefinition) {
            val o_column = net.forestany.forestj.lib.sql.ColumnStructure(o_queryCreate)
            o_column.columnTypeAllocation(o_columnDefinition.getProperty("columnType"))
            o_column.s_name = o_columnDefinition.getProperty("name")
            o_column.setAlterOperation("ADD")

            if (o_columnDefinition.containsKey("constraints")) {
                val a_constraints = o_columnDefinition.getProperty("constraints").split(";").toTypedArray()

                for (s_foo: String in a_constraints) {
                    o_column.addConstraint(o_queryCreate.constraintTypeAllocation(s_foo))

                    if ((s_foo.compareTo("DEFAULT") == 0) && (o_columnDefinition.containsKey("constraintDefaultValue"))) {
                        o_column.constraintDefaultValue = o_columnDefinition.getProperty("constraintDefaultValue")
                    }
                }
            }

            o_queryCreate.query.a_columns.add(o_column)
        }

        var a_result: List<LinkedHashMap<String, Any>> = glob.fJGlob.Base.fetchQuery(o_queryCreate)

        if (a_result.size != 1) {
            Log.e(TAG, "Result row amount of create query is not '1', it is '${a_result.size}'")
        } else {
            val o_resultEntry: Map.Entry<String, Any> = a_result[0].entries.first()

            if (o_resultEntry.value != 0) {
                Log.e(TAG, "Result row value of create query is not '0', it is '${o_resultEntry.value}'")
            }
        }

        /* #### ALTER ########################################################################################### */
//        val o_queryAlter = net.forestany.forestj.lib.sql.Query<net.forestany.forestj.lib.sql.Alter>(glob.fJGlob.BaseGateway, net.forestany.forestj.lib.sqlcore.SqlType.ALTER, tableName)
//
//        /* #### Constraints ##################################################################################### */
//        val o_constraint = net.forestany.forestj.lib.sql.Constraint(o_queryAlter, "UNIQUE", "mediacollection_unique", "", "ADD")
//        o_constraint.a_columns.add("OriginalTitle")
//        o_constraint.a_columns.add("PublicationYear")
//        o_constraint.a_columns.add("Deleted")
//
//        o_queryAlter.query.a_constraints.add(o_constraint)
//
//        a_result = glob.fJGlob.Base.fetchQuery(o_queryAlter)
//
//        if (a_result.size != 1) {
//            Log.e(TAG, "Result row amount of alter query is not '1', it is '${a_result.size}'")
//        } else {
//            val o_resultEntry: Map.Entry<String, Any> = a_result[0].entries.first()
//
//            if (o_resultEntry.value != 0) {
//                Log.e(TAG, "Result row value of alter query is not '0', it is '${o_resultEntry.value}'")
//            }
//        }

        /* #### CREATE ############################################################################# */
        o_queryCreate = net.forestany.forestj.lib.sql.Query<net.forestany.forestj.lib.sql.Create>(glob.fJGlob.BaseGateway, net.forestany.forestj.lib.sqlcore.SqlType.CREATE, tableNameLanguages)

        /* #### Columns ############################################################################ */
        a_columnsDefinition = ArrayList()

        o_properties = Properties()
        o_properties["name"] = "Id"
        o_properties["columnType"] = "integer [int]"
        o_properties["constraints"] = "NOT NULL;PRIMARY KEY;AUTO_INCREMENT"
        a_columnsDefinition.add(o_properties)

        o_properties = Properties()
        o_properties["name"] = "Language"
        o_properties["columnType"] = "text [255]"
        o_properties["constraints"] = "NOT NULL;UNIQUE"
        a_columnsDefinition.add(o_properties)

        /* #### Query ############################################################################ */
        for (o_columnDefinition in a_columnsDefinition) {
            val o_column = net.forestany.forestj.lib.sql.ColumnStructure(o_queryCreate)
            o_column.columnTypeAllocation(o_columnDefinition.getProperty("columnType"))
            o_column.s_name = o_columnDefinition.getProperty("name")
            o_column.setAlterOperation("ADD")

            if (o_columnDefinition.containsKey("constraints")) {
                val a_constraints = o_columnDefinition.getProperty("constraints").split(";").toTypedArray()

                for (s_foo: String in a_constraints) {
                    o_column.addConstraint(o_queryCreate.constraintTypeAllocation(s_foo))

                    if ((s_foo.compareTo("DEFAULT") == 0) && (o_columnDefinition.containsKey("constraintDefaultValue"))) {
                        o_column.constraintDefaultValue = o_columnDefinition.getProperty("constraintDefaultValue")
                    }
                }
            }

            o_queryCreate.query.a_columns.add(o_column)
        }

        a_result = glob.fJGlob.Base.fetchQuery(o_queryCreate)

        if (a_result.size != 1) {
            Log.e(TAG, "Result row amount of create query is not '1', it is '${a_result.size}'")
        } else {
            val o_resultEntry: Map.Entry<String, Any> = a_result[0].entries.first()

            if (o_resultEntry.value != 0) {
                Log.e(TAG, "Result row value of create query is not '0', it is '${o_resultEntry.value}'")
            }
        }

        // insert standard languages
        val o_record = LanguageRecord()

        o_record.ColumnLanguage = "German"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "English"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Japanese"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "French"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Spanish"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Korean"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Italian"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Hindi"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Russian"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Swedish"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

        o_record.ColumnLanguage = "Thai"
        if (o_record.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }
    }
}