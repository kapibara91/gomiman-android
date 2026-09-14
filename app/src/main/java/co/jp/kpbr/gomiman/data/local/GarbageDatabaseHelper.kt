package co.jp.kpbr.gomiman.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel

class GarbageDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "garbage_main.db"
        const val DATABASE_VERSION = 1

        const val TABLE_COLLECTION = "garbage_collection"
        const val TABLE_COLLECTION_TYPE = "garbage_collection_type"
        const val TABLE_COLLECTION_DAY = "garbage_collection_day"
        const val TABLE_COLLECTION_WEEK = "garbage_collection_week"

        const val COLUMN_ID = "id"
        const val COLUMN_WEEKLY_STATUS = "weekly_status"
        const val COLUMN_GARBAGE_COLLECTION_ID = "garbage_collection_id"
        const val COLUMN_GARBAGE_TYPE = "garbage_type"
        const val COLUMN_DAY = "day"
        const val COLUMN_WEEK = "week"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_COLLECTION (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WEEKLY_STATUS INTEGER
            );
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_COLLECTION_TYPE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GARBAGE_COLLECTION_ID INTEGER,
                $COLUMN_GARBAGE_TYPE INTEGER
            );
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_COLLECTION_DAY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GARBAGE_COLLECTION_ID INTEGER,
                $COLUMN_DAY INTEGER
            );
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_COLLECTION_WEEK (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GARBAGE_COLLECTION_ID INTEGER,
                $COLUMN_WEEK INTEGER
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COLLECTION_WEEK")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COLLECTION_DAY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COLLECTION_TYPE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COLLECTION")
        onCreate(db)
    }

    fun insertGarbageCollection(model: GarbageCollectionModel): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put(COLUMN_WEEKLY_STATUS, model.weekStatus)
            }
            val collectionId = db.insert(TABLE_COLLECTION, null, cv)

            for (type in model.garbageTypes) {
                val typeCv = ContentValues().apply {
                    put(COLUMN_GARBAGE_COLLECTION_ID, collectionId)
                    put(COLUMN_GARBAGE_TYPE, type)
                }
                db.insert(TABLE_COLLECTION_TYPE, null, typeCv)
            }

            for (day in model.days) {
                val dayCv = ContentValues().apply {
                    put(COLUMN_GARBAGE_COLLECTION_ID, collectionId)
                    put(COLUMN_DAY, day)
                }
                db.insert(TABLE_COLLECTION_DAY, null, dayCv)
            }

            if (model.weekStatus == GarbageCollectionModel.WEEK_STATUS_BIWEEKLY) {
                for (week in model.weeks) {
                    val weekCv = ContentValues().apply {
                        put(COLUMN_GARBAGE_COLLECTION_ID, collectionId)
                        put(COLUMN_WEEK, week)
                    }
                    db.insert(TABLE_COLLECTION_WEEK, null, weekCv)
                }
            }

            db.setTransactionSuccessful()
            model.id = collectionId
            return collectionId
        } finally {
            db.endTransaction()
        }
    }

    fun getAllGarbageCollections(): List<GarbageCollectionModel> {
        val db = readableDatabase
        val list = mutableListOf<GarbageCollectionModel>()

        val cursor = db.query(TABLE_COLLECTION, null, null, null, null, null, null)
        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
            val weeklyStatusIndex = c.getColumnIndexOrThrow(COLUMN_WEEKLY_STATUS)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val weeklyStatus = c.getInt(weeklyStatusIndex)
                val model = GarbageCollectionModel(
                    id = id,
                    weekStatus = weeklyStatus
                )

                // Query types
                val typeCursor = db.query(
                    TABLE_COLLECTION_TYPE,
                    arrayOf(COLUMN_GARBAGE_TYPE),
                    "$COLUMN_GARBAGE_COLLECTION_ID = ?",
                    arrayOf(id.toString()),
                    null, null, null
                )
                typeCursor.use { tc ->
                    val typeIdx = tc.getColumnIndexOrThrow(COLUMN_GARBAGE_TYPE)
                    while (tc.moveToNext()) {
                        model.garbageTypes.add(tc.getInt(typeIdx))
                    }
                }

                // Query days
                val dayCursor = db.query(
                    TABLE_COLLECTION_DAY,
                    arrayOf(COLUMN_DAY),
                    "$COLUMN_GARBAGE_COLLECTION_ID = ?",
                    arrayOf(id.toString()),
                    null, null, null
                )
                dayCursor.use { dc ->
                    val dayIdx = dc.getColumnIndexOrThrow(COLUMN_DAY)
                    while (dc.moveToNext()) {
                        model.days.add(dc.getInt(dayIdx))
                    }
                }

                // Query weeks
                val weekCursor = db.query(
                    TABLE_COLLECTION_WEEK,
                    arrayOf(COLUMN_WEEK),
                    "$COLUMN_GARBAGE_COLLECTION_ID = ?",
                    arrayOf(id.toString()),
                    null, null, null
                )
                weekCursor.use { wc ->
                    val weekIdx = wc.getColumnIndexOrThrow(COLUMN_WEEK)
                    while (wc.moveToNext()) {
                        model.weeks.add(wc.getInt(weekIdx))
                    }
                }

                list.add(model)
            }
        }
        return list
    }

    fun deleteGarbageCollection(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_COLLECTION, "$COLUMN_ID = ?", arrayOf(id.toString()))
            db.delete(TABLE_COLLECTION_TYPE, "$COLUMN_GARBAGE_COLLECTION_ID = ?", arrayOf(id.toString()))
            db.delete(TABLE_COLLECTION_DAY, "$COLUMN_GARBAGE_COLLECTION_ID = ?", arrayOf(id.toString()))
            db.delete(TABLE_COLLECTION_WEEK, "$COLUMN_GARBAGE_COLLECTION_ID = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteAllGarbageCollections() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_COLLECTION, null, null)
            db.delete(TABLE_COLLECTION_TYPE, null, null)
            db.delete(TABLE_COLLECTION_DAY, null, null)
            db.delete(TABLE_COLLECTION_WEEK, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
