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
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class QuarkAccountDao_Impl(
  __db: RoomDatabase,
) : QuarkAccountDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfQuarkAccountEntity: EntityInsertAdapter<QuarkAccountEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfQuarkAccountEntity = object : EntityInsertAdapter<QuarkAccountEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `quark_account` (`id`,`cookie`,`nickname`,`updatedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: QuarkAccountEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.cookie)
        statement.bindText(3, entity.nickname)
        statement.bindLong(4, entity.updatedAt)
      }
    }
  }

  public override suspend fun upsert(account: QuarkAccountEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfQuarkAccountEntity.insert(_connection, account)
  }

  public override fun observeAccount(): Flow<QuarkAccountEntity?> {
    val _sql: String = "SELECT * FROM quark_account WHERE id = 'quark'"
    return createFlow(__db, false, arrayOf("quark_account")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCookie: Int = getColumnIndexOrThrow(_stmt, "cookie")
        val _columnIndexOfNickname: Int = getColumnIndexOrThrow(_stmt, "nickname")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: QuarkAccountEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCookie: String
          _tmpCookie = _stmt.getText(_columnIndexOfCookie)
          val _tmpNickname: String
          _tmpNickname = _stmt.getText(_columnIndexOfNickname)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = QuarkAccountEntity(_tmpId,_tmpCookie,_tmpNickname,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAccount(): QuarkAccountEntity? {
    val _sql: String = "SELECT * FROM quark_account WHERE id = 'quark'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCookie: Int = getColumnIndexOrThrow(_stmt, "cookie")
        val _columnIndexOfNickname: Int = getColumnIndexOrThrow(_stmt, "nickname")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: QuarkAccountEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCookie: String
          _tmpCookie = _stmt.getText(_columnIndexOfCookie)
          val _tmpNickname: String
          _tmpNickname = _stmt.getText(_columnIndexOfNickname)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = QuarkAccountEntity(_tmpId,_tmpCookie,_tmpNickname,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clear() {
    val _sql: String = "DELETE FROM quark_account WHERE id = 'quark'"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
