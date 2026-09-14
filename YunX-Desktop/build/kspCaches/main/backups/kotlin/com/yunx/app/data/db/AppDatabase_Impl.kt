package com.yunx.app.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _quarkAccountDao: Lazy<QuarkAccountDao> = lazy {
    QuarkAccountDao_Impl(this)
  }

  private val _baiduAccountDao: Lazy<BaiduAccountDao> = lazy {
    BaiduAccountDao_Impl(this)
  }

  private val _downloadTaskDao: Lazy<DownloadTaskDao> = lazy {
    DownloadTaskDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "67c3a3fbae46e38b37c006282c37d0b4", "75add6dc4a6920abaff974b815eadea2") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `quark_account` (`id` TEXT NOT NULL, `cookie` TEXT NOT NULL, `nickname` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `baidu_account` (`id` TEXT NOT NULL, `cookie` TEXT NOT NULL, `nickname` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `download_task` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `fileName` TEXT NOT NULL, `totalSize` INTEGER NOT NULL, `downloadedSize` INTEGER NOT NULL, `status` INTEGER NOT NULL, `errorMsg` TEXT NOT NULL, `savePath` TEXT NOT NULL, `requestHeadersJson` TEXT NOT NULL DEFAULT '{}', `chunkCount` INTEGER NOT NULL DEFAULT 0, `plannedTotalSize` INTEGER NOT NULL DEFAULT 0, `cleanupId` TEXT NOT NULL DEFAULT '', `platform` TEXT NOT NULL DEFAULT '', `avgSpeed` INTEGER NOT NULL DEFAULT 0, `createTime` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '67c3a3fbae46e38b37c006282c37d0b4')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `quark_account`")
        connection.execSQL("DROP TABLE IF EXISTS `baidu_account`")
        connection.execSQL("DROP TABLE IF EXISTS `download_task`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsQuarkAccount: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsQuarkAccount.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuarkAccount.put("cookie", TableInfo.Column("cookie", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuarkAccount.put("nickname", TableInfo.Column("nickname", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuarkAccount.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysQuarkAccount: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesQuarkAccount: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoQuarkAccount: TableInfo = TableInfo("quark_account", _columnsQuarkAccount,
            _foreignKeysQuarkAccount, _indicesQuarkAccount)
        val _existingQuarkAccount: TableInfo = read(connection, "quark_account")
        if (!_infoQuarkAccount.equals(_existingQuarkAccount)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |quark_account(com.yunx.app.data.db.QuarkAccountEntity).
              | Expected:
              |""".trimMargin() + _infoQuarkAccount + """
              |
              | Found:
              |""".trimMargin() + _existingQuarkAccount)
        }
        val _columnsBaiduAccount: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBaiduAccount.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBaiduAccount.put("cookie", TableInfo.Column("cookie", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBaiduAccount.put("nickname", TableInfo.Column("nickname", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBaiduAccount.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBaiduAccount: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBaiduAccount: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBaiduAccount: TableInfo = TableInfo("baidu_account", _columnsBaiduAccount,
            _foreignKeysBaiduAccount, _indicesBaiduAccount)
        val _existingBaiduAccount: TableInfo = read(connection, "baidu_account")
        if (!_infoBaiduAccount.equals(_existingBaiduAccount)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |baidu_account(com.yunx.app.data.db.BaiduAccountEntity).
              | Expected:
              |""".trimMargin() + _infoBaiduAccount + """
              |
              | Found:
              |""".trimMargin() + _existingBaiduAccount)
        }
        val _columnsDownloadTask: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDownloadTask.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("url", TableInfo.Column("url", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("fileName", TableInfo.Column("fileName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("totalSize", TableInfo.Column("totalSize", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("downloadedSize", TableInfo.Column("downloadedSize", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("status", TableInfo.Column("status", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("errorMsg", TableInfo.Column("errorMsg", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("savePath", TableInfo.Column("savePath", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("requestHeadersJson", TableInfo.Column("requestHeadersJson",
            "TEXT", true, 0, "'{}'", TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("chunkCount", TableInfo.Column("chunkCount", "INTEGER", true, 0,
            "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("plannedTotalSize", TableInfo.Column("plannedTotalSize", "INTEGER",
            true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("cleanupId", TableInfo.Column("cleanupId", "TEXT", true, 0, "''",
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("platform", TableInfo.Column("platform", "TEXT", true, 0, "''",
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("avgSpeed", TableInfo.Column("avgSpeed", "INTEGER", true, 0, "0",
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTask.put("createTime", TableInfo.Column("createTime", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDownloadTask: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDownloadTask: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDownloadTask: TableInfo = TableInfo("download_task", _columnsDownloadTask,
            _foreignKeysDownloadTask, _indicesDownloadTask)
        val _existingDownloadTask: TableInfo = read(connection, "download_task")
        if (!_infoDownloadTask.equals(_existingDownloadTask)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |download_task(com.yunx.app.data.db.DownloadTaskEntity).
              | Expected:
              |""".trimMargin() + _infoDownloadTask + """
              |
              | Found:
              |""".trimMargin() + _existingDownloadTask)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "quark_account",
        "baidu_account", "download_task")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(QuarkAccountDao::class, QuarkAccountDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BaiduAccountDao::class, BaiduAccountDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DownloadTaskDao::class, DownloadTaskDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun rawQuarkAccountDao(): QuarkAccountDao = _quarkAccountDao.value

  public override fun rawBaiduAccountDao(): BaiduAccountDao = _baiduAccountDao.value

  public override fun downloadTaskDao(): DownloadTaskDao = _downloadTaskDao.value
}
