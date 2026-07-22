package com.example.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "exam_history")
data class ExamHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentName: String,
    val studentClass: String,
    val examCode: String,
    val totalPoints: Double,
    val p1Points: Double,
    val p2Points: Double,
    val p3Points: Double,
    val timeElapsedSeconds: Int,
    val timestamp: Long,
    val isPracticeMode: Boolean,
    
    // Serialized questions and answers
    val part1QuestionsJson: String,
    val part2QuestionsJson: String,
    val part3QuestionsJson: String,
    val part1AnswersJson: String,
    val part2AnswersJson: String,
    val part3AnswersJson: String
)

@Entity(tableName = "math_questions")
data class MathQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int, // 0 = A, 1 = B, 2 = C, 3 = D
    val explanation: String,
    val topic: String = "Giải tích", // e.g. "Đại số", "Hình học", "Giải tích", "Lượng giác", "Xác suất"
    val tagsJson: String = "[\"Algebra\", \"Calculus\"]", // JSON string array of tags
    val difficulty: String = "Dễ", // "Dễ", "Trung bình", "Khó", "Vận dụng cao"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTagsList(): List<String> {
        return try {
            val array = JSONArray(tagsJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun tagsListToJson(tags: List<String>): String {
            val array = JSONArray()
            tags.forEach { array.put(it) }
            return array.toString()
        }
    }
}

@Dao
interface ExamHistoryDao {
    @Query("SELECT * FROM exam_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ExamHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: ExamHistoryEntity)

    @Query("DELETE FROM exam_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)
}

@Dao
interface MathQuestionDao {
    @Query("SELECT * FROM math_questions ORDER BY createdAt DESC")
    fun getAllQuestions(): Flow<List<MathQuestionEntity>>

    @Query("SELECT * FROM math_questions WHERE id = :id")
    suspend fun getQuestionById(id: Int): MathQuestionEntity?

    @Query("SELECT * FROM math_questions WHERE content LIKE '%' || :query || '%' OR explanation LIKE '%' || :query || '%' OR topic LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchQuestions(query: String): Flow<List<MathQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: MathQuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<MathQuestionEntity>)

    @Update
    suspend fun updateQuestion(question: MathQuestionEntity)

    @Query("DELETE FROM math_questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Int)

    @Query("DELETE FROM math_questions")
    suspend fun deleteAllQuestions()
}

@Database(entities = [ExamHistoryEntity::class, MathQuestionEntity::class], version = 2, exportSchema = false)
abstract class ExamDatabase : RoomDatabase() {
    abstract fun examHistoryDao(): ExamHistoryDao
    abstract fun mathQuestionDao(): MathQuestionDao

    companion object {
        @Volatile
        private var INSTANCE: ExamDatabase? = null

        fun getDatabase(context: Context): ExamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamDatabase::class.java,
                    "exam_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ExamHistoryRepository(private val dao: ExamHistoryDao) {
    val allHistory: Flow<List<ExamHistoryEntity>> = dao.getAllHistory()

    suspend fun insert(entity: ExamHistoryEntity) {
        dao.insertHistory(entity)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteHistoryById(id)
    }
}

class MathQuestionRepository(private val dao: MathQuestionDao) {
    val allQuestions: Flow<List<MathQuestionEntity>> = dao.getAllQuestions()

    fun searchQuestions(query: String): Flow<List<MathQuestionEntity>> = dao.searchQuestions(query)

    suspend fun insert(question: MathQuestionEntity): Long = dao.insertQuestion(question)

    suspend fun insertAll(questions: List<MathQuestionEntity>) = dao.insertQuestions(questions)

    suspend fun update(question: MathQuestionEntity) = dao.updateQuestion(question)

    suspend fun deleteById(id: Int) = dao.deleteQuestionById(id)

    suspend fun deleteAll() = dao.deleteAllQuestions()
}

// Custom manual JSON serializer for complete compile-time type safety without annotation issues
object ExamJsonSerializer {
    
    // Part 1
    fun part1QuestionsToJson(questions: List<com.example.ShuffledPart1Question>): String {
        val array = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject()
            val innerQ = JSONObject()
            innerQ.put("id", q.question.id)
            innerQ.put("content", q.question.content)
            
            val origOpts = JSONArray()
            q.question.originalOptions.forEach { origOpts.put(it) }
            innerQ.put("originalOptions", origOpts)
            innerQ.put("correctIndex", q.question.correctIndex)
            innerQ.put("explanation", q.question.explanation)
            innerQ.put("topic", q.question.topic)
            innerQ.put("difficulty", q.question.difficulty)
            
            obj.put("question", innerQ)
            
            val opts = JSONArray()
            q.options.forEach { opts.put(it) }
            obj.put("options", opts)
            obj.put("correctIndex", q.correctIndex)
            
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToPart1Questions(jsonStr: String): List<com.example.ShuffledPart1Question> {
        val list = mutableListOf<com.example.ShuffledPart1Question>()
        if (jsonStr.isEmpty()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val innerQObj = obj.getJSONObject("question")
                
                val origOptsArray = innerQObj.getJSONArray("originalOptions")
                val origOpts = mutableListOf<String>()
                for (j in 0 until origOptsArray.length()) {
                    origOpts.add(origOptsArray.getString(j))
                }
                
                val question = com.example.Part1Question(
                    id = innerQObj.getInt("id"),
                    content = innerQObj.getString("content"),
                    originalOptions = origOpts,
                    correctIndex = innerQObj.getInt("correctIndex"),
                    explanation = innerQObj.getString("explanation"),
                    topic = if (innerQObj.has("topic")) innerQObj.getString("topic") else "Giải tích",
                    difficulty = if (innerQObj.has("difficulty")) innerQObj.getString("difficulty") else "Dễ"
                )
                
                val optsArray = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optsArray.length()) {
                    opts.add(optsArray.getString(j))
                }
                
                list.add(
                    com.example.ShuffledPart1Question(
                        question = question,
                        options = opts,
                        correctIndex = obj.getInt("correctIndex")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Part 2
    fun part2QuestionsToJson(questions: List<com.example.Part2Question>): String {
        val array = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject()
            obj.put("id", q.id)
            obj.put("content", q.content)
            obj.put("explanation", q.explanation)
            obj.put("topic", q.topic)
            obj.put("difficulty", q.difficulty)
            
            val subArray = JSONArray()
            q.subQuestions.forEach { sq ->
                val subObj = JSONObject()
                subObj.put("statement", sq.statement)
                subObj.put("isTrue", sq.isTrue)
                subArray.put(subObj)
            }
            obj.put("subQuestions", subArray)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToPart2Questions(jsonStr: String): List<com.example.Part2Question> {
        val list = mutableListOf<com.example.Part2Question>()
        if (jsonStr.isEmpty()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val subArray = obj.getJSONArray("subQuestions")
                val subs = mutableListOf<com.example.Part2SubQuestion>()
                for (j in 0 until subArray.length()) {
                    val subObj = subArray.getJSONObject(j)
                    subs.add(
                        com.example.Part2SubQuestion(
                            statement = subObj.getString("statement"),
                            isTrue = subObj.getBoolean("isTrue")
                        )
                    )
                }
                list.add(
                    com.example.Part2Question(
                        id = obj.getInt("id"),
                        content = obj.getString("content"),
                        subQuestions = subs,
                        explanation = obj.getString("explanation"),
                        topic = if (obj.has("topic")) obj.getString("topic") else "Giải tích",
                        difficulty = if (obj.has("difficulty")) obj.getString("difficulty") else "Trung bình"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Part 3
    fun part3QuestionsToJson(questions: List<com.example.Part3Question>): String {
        val array = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject()
            obj.put("id", q.id)
            obj.put("content", q.content)
            obj.put("correctAnswer", q.correctAnswer)
            obj.put("explanation", q.explanation)
            obj.put("topic", q.topic)
            obj.put("difficulty", q.difficulty)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToPart3Questions(jsonStr: String): List<com.example.Part3Question> {
        val list = mutableListOf<com.example.Part3Question>()
        if (jsonStr.isEmpty()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.Part3Question(
                        id = obj.getInt("id"),
                        content = obj.getString("content"),
                        correctAnswer = obj.getDouble("correctAnswer"),
                        explanation = obj.getString("explanation"),
                        topic = if (obj.has("topic")) obj.getString("topic") else "Giải tích",
                        difficulty = if (obj.has("difficulty")) obj.getString("difficulty") else "Khó"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Answers serialization
    fun part1AnswersToJson(answers: Map<Int, Int>): String {
        val obj = JSONObject()
        answers.forEach { (k, v) ->
            obj.put(k.toString(), v)
        }
        return obj.toString()
    }

    fun jsonToPart1Answers(jsonStr: String): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        if (jsonStr.isEmpty()) return map
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k.toInt()] = obj.getInt(k)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun part2AnswersToJson(answers: Map<Int, List<Boolean?>>): String {
        val obj = JSONObject()
        answers.forEach { (qId, list) ->
            val arr = JSONArray()
            list.forEach { ans ->
                if (ans == null) arr.put(JSONObject.NULL) else arr.put(ans)
            }
            obj.put(qId.toString(), arr)
        }
        return obj.toString()
    }

    fun jsonToPart2Answers(jsonStr: String): Map<Int, List<Boolean?>> {
        val map = mutableMapOf<Int, List<Boolean?>>()
        if (jsonStr.isEmpty()) return map
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val qId = keys.next()
                val arr = obj.getJSONArray(qId)
                val list = mutableListOf<Boolean?>()
                for (i in 0 until arr.length()) {
                    if (arr.isNull(i)) {
                        list.add(null)
                    } else {
                        list.add(arr.getBoolean(i))
                    }
                }
                map[qId.toInt()] = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun part3AnswersToJson(answers: Map<Int, String>): String {
        val obj = JSONObject()
        answers.forEach { (k, v) ->
            obj.put(k.toString(), v)
        }
        return obj.toString()
    }

    fun jsonToPart3Answers(jsonStr: String): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        if (jsonStr.isEmpty()) return map
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k.toInt()] = obj.getString(k)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
}
