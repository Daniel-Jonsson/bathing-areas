package se.miun.dajo1903.dt031g.bathingsites

/**
 * The DatabaseHelperImpl is an convenient class that implements the BathingSiteDao. This makes it a
 * tiny bit easier to call the databasehelper directly instead of using
 * appDatabase::BathingSiteDao::getBathingSites for example. Instead this can be done
 * databaseHelper::getBathingSites which looks cleaner.
 */
class DatabaseHelperImpl(private val appDatabase: AppDatabase) : BathingSiteDao {

    override suspend fun getBathingSites(): List<BathingSite> = appDatabase.BathingSiteDao().getBathingSites()
    override suspend fun insertOne(bathingSite: BathingSite) = appDatabase.BathingSiteDao().insertOne(bathingSite)

    override suspend fun insertMany(bathingSites: List<BathingSite>) = appDatabase.BathingSiteDao().insertMany(bathingSites)
}