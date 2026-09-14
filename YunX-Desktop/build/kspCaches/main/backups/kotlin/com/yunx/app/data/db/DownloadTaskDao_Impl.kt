package com.yunx.app.`data`.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DownloadTaskDao_Impl(
  __db: RoomDatabase,
) : DownloadTaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDownloadTaskEntity: EntityInsertAdapter<DownloadTaskEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfDownloadTaskEntity = object : EntityInsertAdapter<DownloadTaskEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `download_task` (`id`,`url`,`fileName`,`totalSize`,`downloadedSize`,`status`,`errorMsg`,`savePath`,`requestHeadersJson`,`chunkCount`,`plannedTotalSize`,`cleanupId`,`platform`,`avgSpeed`,`createTime`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DownloadTaskEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.url)
        statement.bindText(3, entity.fileName)
        statement.bindLong(4, entity.totalSize)
        statement.bindLong(5, entity.downloadedSize)
        statement.bindLong(6, entity.status.toLong())
        statement.bindText(7, entity.errorMsg)
        statement.bindText(8, entity.savePath)
        statement.bindText(9, entity.requestHeadersJson)
        statement.bindLong(10, entity.chunkCount.toLong())
        statement.bindLong(11, entity.plannedTotalSize)
        statement.bindText(12, entity.cleanupId)
        statement.bindText(13, entity.platform)
        statement.bindLong(14, entity.avgSpeed)
        statement.bindLong(15, entity.createTime)
      }
    }
  }

  public override suspend fun insert(task: DownloadTaskEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfDownloadTaskEntity.insertAndReturnId(_connection, task)
    _result
  }

  public override fun observeAll(): Flow<List<DownloadTaskEntity>> {
    val _sql: String = "SELECT * FROM download_task ORDER BY createTime DESC"
    return createFlow(__db, false, arrayOf("download_task")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "fileName")
        val _columnIndexOfTotalSize: Int = getColumnIndexOrThrow(_stmt, "totalSize")
        val _columnIndexOfDownloadedSize: Int = getColumnIndexOrThrow(_stmt, "downloadedSize")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfErrorMsg: Int = getColumnIndexOrThrow(_stmt, "errorMsg")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "savePath")
        val _columnIndexOfRequestHeadersJson: Int = getColumnIndexOrThrow(_stmt,
            "requestHeadersJson")
        val _columnIndexOfChunkCount: Int = getColumnIndexOrThrow(_stmt, "chunkCount")
        val _columnIndexOfPlannedTotalSize: Int = getColumnIndexOrThrow(_stmt, "plannedTotalSize")
        val _columnIndexOfCleanupId: Int = getColumnIndexOrThrow(_stmt, "cleanupId")
        val _columnIndexOfPlatform: Int = getColumnIndexOrThrow(_stmt, "platform")
        val _columnIndexOfAvgSpeed: Int = getColumnIndexOrThrow(_stmt, "avgSpeed")
        val _columnIndexOfCreateTime: Int = getColumnIndexOrThrow(_stmt, "createTime")
        val _result: MutableList<DownloadTaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadTaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpTotalSize: Long
          _tmpTotalSize = _stmt.getLong(_columnIndexOfTotalSize)
          val _tmpDownloadedSize: Long
          _tmpDownloadedSize = _stmt.getLong(_columnIndexOfDownloadedSize)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpErrorMsg: String
          _tmpErrorMsg = _stmt.getText(_columnIndexOfErrorMsg)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpRequestHeadersJson: String
          _tmpRequestHeadersJson = _stmt.getText(_columnIndexOfRequestHeadersJson)
          val _tmpChunkCount: Int
          _tmpChunkCount = _stmt.getLong(_columnIndexOfChunkCount).toInt()
          val _tmpPlannedTotalSize: Long
          _tmpPlannedTotalSize = _stmt.getLong(_columnIndexOfPlannedTotalSize)
          val _tmpCleanupId: String
          _tmpCleanupId = _stmt.getText(_columnIndexOfCleanupId)
          val _tmpPlatform: String
          _tmpPlatform = _stmt.getText(_columnIndexOfPlatform)
          val _tmpAvgSpeed: Long
          _tmpAvgSpeed = _stmt.getLong(_columnIndexOfAvgSpeed)
          val _tmpCreateTime: Long
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime)
          _item =
              DownloadTaskEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpTotalSize,_tmpDownloadedSize,_tmpStatus,_tmpErrorMsg,_tmpSavePath,_tmpRequestHeadersJson,_tmpChunkCount,_tmpPlannedTotalSize,_tmpCleanupId,_tmpPlatform,_tmpAvgSpeed,_tmpCreateTime)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun `get`(id: Long): DownloadTaskEntity? {
    val _sql: String = "SELECT * FROM download_task WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "fileName")
        val _columnIndexOfTotalSize: Int = getColumnIndexOrThrow(_stmt, "totalSize")
        val _columnIndexOfDownloadedSize: Int = getColumnIndexOrThrow(_stmt, "downloadedSize")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfErrorMsg: Int = getColumnIndexOrThrow(_stmt, "errorMsg")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "savePath")
        val _columnIndexOfRequestHeadersJson: Int = getColumnIndexOrThrow(_stmt,
            "requestHeadersJson")
        val _columnIndexOfChunkCount: Int = getColumnIndexOrThrow(_stmt, "chunkCount")
        val _columnIndexOfPlannedTotalSize: Int = getColumnIndexOrThrow(_stmt, "plannedTotalSize")
        val _columnIndexOfCleanupId: Int = getColumnIndexOrThrow(_stmt, "cleanupId")
        val _columnIndexOfPlatform: Int = getColumnIndexOrThrow(_stmt, "platform")
        val _columnIndexOfAvgSpeed: Int = getColumnIndexOrThrow(_stmt, "avgSpeed")
        val _columnIndexOfCreateTime: Int = getColumnIndexOrThrow(_stmt, "createTime")
        val _result: DownloadTaskEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpTotalSize: Long
          _tmpTotalSize = _stmt.getLong(_columnIndexOfTotalSize)
          val _tmpDownloadedSize: Long
          _tmpDownloadedSize = _stmt.getLong(_columnIndexOfDownloadedSize)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpErrorMsg: String
          _tmpErrorMsg = _stmt.getText(_columnIndexOfErrorMsg)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpRequestHeadersJson: String
          _tmpRequestHeadersJson = _stmt.getText(_columnIndexOfRequestHeadersJson)
          val _tmpChunkCount: Int
          _tmpChunkCount = _stmt.getLong(_columnIndexOfChunkCount).toInt()
          val _tmpPlannedTotalSize: Long
          _tmpPlannedTotalSize = _stmt.getLong(_columnIndexOfPlannedTotalSize)
          val _tmpCleanupId: String
          _tmpCleanupId = _stmt.getText(_columnIndexOfCleanupId)
          val _tmpPlatform: String
          _tmpPlatform = _stmt.getText(_columnIndexOfPlatform)
          val _tmpAvgSpeed: Long
          _tmpAvgSpeed = _stmt.getLong(_columnIndexOfAvgSpeed)
          val _tmpCreateTime: Long
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime)
          _result =
              DownloadTaskEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpTotalSize,_tmpDownloadedSize,_tmpStatus,_tmpErrorMsg,_tmpSavePath,_tmpRequestHeadersJson,_tmpChunkCount,_tmpPlannedTotalSize,_tmpCleanupId,_tmpPlatform,_tmpAvgSpeed,_tmpCreateTime)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getInterrupted(): List<DownloadTaskEntity> {
    val _sql: String = "SELECT * FROM download_task WHERE status = 1 OR status = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "fileName")
        val _columnIndexOfTotalSize: Int = getColumnIndexOrThrow(_stmt, "totalSize")
        val _columnIndexOfDownloadedSize: Int = getColumnIndexOrThrow(_stmt, "downloadedSize")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfErrorMsg: Int = getColumnIndexOrThrow(_stmt, "errorMsg")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "savePath")
        val _columnIndexOfRequestHeadersJson: Int = getColumnIndexOrThrow(_stmt,
            "requestHeadersJson")
        val _columnIndexOfChunkCount: Int = getColumnIndexOrThrow(_stmt, "chunkCount")
        val _columnIndexOfPlannedTotalSize: Int = getColumnIndexOrThrow(_stmt, "plannedTotalSize")
        val _columnIndexOfCleanupId: Int = getColumnIndexOrThrow(_stmt, "cleanupId")
        val _columnIndexOfPlatform: Int = getColumnIndexOrThrow(_stmt, "platform")
        val _columnIndexOfAvgSpeed: Int = getColumnIndexOrThrow(_stmt, "avgSpeed")
        val _columnIndexOfCreateTime: Int = getColumnIndexOrThrow(_stmt, "createTime")
        val _result: MutableList<DownloadTaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadTaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpTotalSize: Long
          _tmpTotalSize = _stmt.getLong(_columnIndexOfTotalSize)
          val _tmpDownloadedSize: Long
          _tmpDownloadedSize = _stmt.getLong(_columnIndexOfDownloadedSize)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpErrorMsg: String
          _tmpErrorMsg = _stmt.getText(_columnIndexOfErrorMsg)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpRequestHeadersJson: String
          _tmpRequestHeadersJson = _stmt.getText(_columnIndexOfRequestHeadersJson)
          val _tmpChunkCount: Int
          _tmpChunkCount = _stmt.getLong(_columnIndexOfChunkCount).toInt()
          val _tmpPlannedTotalSize: Long
          _tmpPlannedTotalSize = _stmt.getLong(_columnIndexOfPlannedTotalSize)
          val _tmpCleanupId: String
          _tmpCleanupId = _stmt.getText(_columnIndexOfCleanupId)
          val _tmpPlatform: String
          _tmpPlatform = _stmt.getText(_columnIndexOfPlatform)
          val _tmpAvgSpeed: Long
          _tmpAvgSpeed = _stmt.getLong(_columnIndexOfAvgSpeed)
          val _tmpCreateTime: Long
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime)
          _item =
              DownloadTaskEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpTotalSize,_tmpDownloadedSize,_tmpStatus,_tmpErrorMsg,_tmpSavePath,_tmpRequestHeadersJson,_tmpChunkCount,_tmpPlannedTotalSize,_tmpCleanupId,_tmpPlatform,_tmpAvgSpeed,_tmpCreateTime)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateProgress(
    id: Long,
    status: Int,
    downloadedSize: Long,
    totalSize: Long,
  ) {
    val _sql: String =
        "UPDATE download_task SET status = ?, downloadedSize = ?, totalSize = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, status.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, downloadedSize)
        _argIndex = 3
        _stmt.bindLong(_argIndex, totalSize)
        _argIndex = 4
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updatePlan(
    id: Long,
    chunkCount: Int,
    totalSize: Long,
  ) {
    val _sql: String = "UPDATE download_task SET chunkCount = ?, plannedTotalSize = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, chunkCount.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, totalSize)
        _argIndex = 3
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateRequestHeaders(id: Long, encryptedHeaders: String) {
    val _sql: String = "UPDATE download_task SET requestHeadersJson = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, encryptedHeaders)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markInterruptedAsPaused() {
    val _sql: String = "UPDATE download_task SET status = 2 WHERE status = 1 OR status = 0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(id: Long, status: Int) {
    val _sql: String = "UPDATE download_task SET status = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, status.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateError(id: Long, errorMsg: String) {
    val _sql: String = "UPDATE download_task SET errorMsg = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, errorMsg)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun complete(
    id: Long,
    status: Int,
    savePath: String,
    avgSpeed: Long,
  ) {
    val _sql: String =
        "UPDATE download_task SET status = ?, savePath = ?, avgSpeed = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, status.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, savePath)
        _argIndex = 3
        _stmt.bindLong(_argIndex, avgSpeed)
        _argIndex = 4
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: Long) {
    val _sql: String = "DELETE FROM download_task WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
