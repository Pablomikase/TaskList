package tasklist

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.Exception
import kotlin.system.exitProcess

val taskList = mutableListOf<Task>()

data class Task(
    val taskDate: LocalDateTime,
    val priority: Priority,
    val actions: MutableList<String>
)

data class DBTask(
    val taskDate: String,
    val priority: Priority,
    val actions: MutableList<String>
)

enum class Priority(val abreviation: Char) {
    CRITICAL('C'),
    HIGH('H'),
    NORMAL('N'),
    LOW('L')
}

fun main() {
    startProgram()
}

fun startProgram() {
    loadDataFromJson()
    println("Input an action (add, print, edit, delete, end):")
    when (readln()) {
        "add" -> {
            //Execute add
            executeAddModule()
            startProgram()
        }

        "print" -> {
            taskList.printList()
            startProgram()
        }

        "end" -> {
            saveCurrentListInFile()
            println("Tasklist exiting!")
            exitProcess(0)
        }

        "delete" -> {
            executeDeleteModule()
            startProgram()
        }

        "edit" -> {
            executeEditModule()
            startProgram()
        }

        else -> {
            println("The input action is invalid")
            startProgram()
        }
    }
}

fun loadDataFromJson() {
    val taskListFile = File("tasklist.json")
    if (!taskListFile.exists()) return
    val rowJson = taskListFile.readText()
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val type = Types.newParameterizedType(MutableList::class.java, DBTask::class.java)
    val taskListAdapter = moshi.adapter<MutableList<DBTask>>(type)
    val result = taskListAdapter.fromJson(rowJson)
    if (result != null) taskList.addAll(result.asUiModel())
}

private fun DBTask.asUIModel(): Task {
    return Task(
        taskDate = LocalDateTime.parse(this.taskDate),
        priority = this.priority,
        actions = this.actions
    )
}

private fun MutableList<DBTask>.asUiModel(): MutableList<Task> {
    val resultList = mutableListOf<Task>()
    this.forEach {
        resultList.add(it.asUIModel())
    }
    return resultList
}

fun saveCurrentListInFile() {

    val taskListFile = File("tasklist.json")
    if (!taskListFile.exists()) taskListFile.createNewFile()
    //Setting up Moshi
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val type = Types.newParameterizedType(MutableList::class.java, DBTask::class.java)
    val taskListAdapter = moshi.adapter<MutableList<DBTask>>(type)

    val result = taskListAdapter.toJson(taskList.asDBModel())
    //println(result)
    taskListFile.writeText(result)
}

private fun Task.asDBModel(): DBTask {
    return DBTask(
        taskDate = this.taskDate.toString(),
        priority = this.priority,
        actions = this.actions
    )
}

private fun MutableList<Task>.asDBModel(): MutableList<DBTask> {
    val resultList = mutableListOf<DBTask>()
    this.forEach {
        resultList.add(it.asDBModel())
    }
    return resultList
}

fun executeEditModule() {
    if (taskList.isEmpty()) {
        println("No tasks have been input")
        return
    }

    val taskId = askForValidTaskId()


    val editedTask = taskList.editTask(taskId)
    println("The task is changed")

}

private fun askForValidTaskId(): Int {
    taskList.printList()
    var selectedIdTask = -1
    gettingTaskNumber@ while (true) {
        println("Input the task number (1-${taskList.size}):")
        try {
            val taskIdToDelete = readln().toInt() - 1
            if (taskIdToDelete !in 0 until taskList.size) {
                println("Invalid task number")
                continue
            } else {
                return taskIdToDelete

            }
        } catch (ex: Exception) {
            println("Invalid task number")
            continue
        }
    }
}

private fun MutableList<Task>.editTask(taskId: Int) {
    println("Input a field to edit (priority, date, time, task):")
    val taskToEdit = this[taskId]
    when (readln()) {
        "priority" -> {
            this[taskId] = taskToEdit.copy(priority = askForPriority())
        }

        "date" -> {
            val currentLocalDateTime = taskToEdit.taskDate
            val newDate = askForDate()
            this[taskId] = taskToEdit.copy(
                taskDate = LocalDateTime(
                    year = newDate.year,
                    monthNumber = newDate.monthNumber,
                    dayOfMonth = newDate.dayOfMonth,
                    hour = currentLocalDateTime.hour,
                    minute = currentLocalDateTime.minute
                )
            )
        }

        "time" -> {
            val currentLocalDateTime = taskToEdit.taskDate
            val newTime = askForTime()
            this[taskId] = taskToEdit.copy(
                taskDate = LocalDateTime(
                    year = currentLocalDateTime.year,
                    monthNumber = currentLocalDateTime.monthNumber,
                    dayOfMonth = currentLocalDateTime.dayOfMonth,
                    hour = newTime.hour,
                    minute = newTime.minute
                )
            )
        }

        "task" -> {
            this[taskId] = taskToEdit.copy(
                actions = askForActions()
            )
        }

        else -> {
            println("Invalid field")
            this.editTask(taskId)
        }
    }
}

fun executeDeleteModule() {
    if (taskList.isEmpty()) {
        println("No tasks have been input")
        return
    }
    val selectedIdTask = askForValidTaskId()
    taskList.removeAt(selectedIdTask)
    println("The task is deleted")

}

private fun MutableList<Task>.printList() {
    if (this.isEmpty()) {
        println("No tasks have been input")
    } else {
        //printing header
        printSeparator()
        printHeadder()
        printSeparator()


        for (index in this.indices) {
            val currentTask = this[index]
            val formater = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm ")
            val dateFormated = currentTask.taskDate.toJavaLocalDateTime().format(formater)
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
            val numberOfDays = currentTask.taskDate.date.daysUntil(currentDate)

            //IndexPrinting
            print("| ${index + 1}${if (index < 9) "  " else " "}")

            //DatePrinting
            print("| $dateFormated")

            //PrintingPriority
            when (currentTask.priority) {
                Priority.CRITICAL -> print("| \u001B[101m \u001B[0m ")
                Priority.HIGH -> print("| \u001B[103m \u001B[0m ")
                Priority.NORMAL -> print("| \u001B[102m \u001B[0m ")
                Priority.LOW -> print("| \u001B[104m \u001B[0m ")
            }

            //Due
            when {
                numberOfDays == 0 -> print("| \u001B[103m \u001B[0m ")
                numberOfDays > 0 -> print("| \u001B[101m \u001B[0m ")
                else -> print("| \u001B[102m \u001B[0m ")
            }

            //Tasks
            for (i in currentTask.actions.indices) {
                val currentAction = currentTask.actions[i]
                print("|")
                if (i == 0) {
                    printActionWithFormat(currentAction)
                } else {
                    print("    |            |       |   |   |")
                    printActionWithFormat(currentAction)
                }
            }

            printSeparator()
        }
    }
}

private fun printActionWithFormat(currentAction: String) {
    var characterCounter = 0
    currentAction.forEach {
        if (it == '\n' || characterCounter == 44) {
            print("|\n|    |            |       |   |   |$it")
            characterCounter = 1
        } else {
            characterCounter++
            print(it)
        }
    }
    repeat(44 - characterCounter) {
        print(" ")
    }
    println("|")
}

fun printHeadder() {
    println("| N  |    Date    | Time  | P | D |                   Task                     |")

}

fun printSeparator() {
    println("+----+------------+-------+---+---+--------------------------------------------+")
}

fun executeAddModule() {

    val prioritySelected = askForPriority()
    val selectedDate = askForDate()
    val selectedTime = askForTime()
    val actions = askForActions()
    val currentTask = Task(
        taskDate = LocalDateTime(
            year = selectedDate.year,
            monthNumber = selectedDate.monthNumber,
            dayOfMonth = selectedDate.dayOfMonth,
            hour = selectedTime.hour,
            minute = selectedTime.minute
        ),
        priority = prioritySelected,
        actions = actions
    )
    if (currentTask.actions.isNotEmpty()) taskList.add(currentTask)
}

fun askForTime(): LocalDateTime {
    println("Input the time (hh:mm):")
    val rowSelectedTime = readln()
    val regexReference = Regex("[0-9]?[0-9]:[0-9]?[0-9]")
    if (!regexReference.matches(rowSelectedTime)) {
        println("The input time is invalid")
        return askForTime()
    }

    val selectedTime = rowSelectedTime.split(':').map { it.toInt() }
    //Validate time
    val hours = selectedTime.first()
    if (hours !in 0..23) {
        println("The input time is invalid")
        return askForTime()
    }
    val minutes = selectedTime.last()
    if (minutes !in 0..59) {
        println("The input time is invalid")
        return askForTime()
    }
    return LocalDateTime(
        year = 2000,
        monthNumber = 1,
        dayOfMonth = 1,
        hour = hours,
        minute = minutes
    )
}

fun askForActions(): MutableList<String> {
    val currentTaskList = mutableListOf<String>()
    println("Input a new task (enter a blank line to end):")
    taskListCollection@ while (true) {
        val currentLine = readln().trim()
        when {
            currentLine.isEmpty() && currentTaskList.isEmpty() -> {
                println("The task is blank")
                break@taskListCollection
            }

            currentLine.isNotEmpty() -> {
                currentTaskList.add(currentLine)
            }

            currentLine.isEmpty() && currentTaskList.isNotEmpty() -> {
                break@taskListCollection
            }
        }
    }

    return currentTaskList
}

fun askForDate(): LocalDateTime {

    println("Input the date (yyyy-mm-dd):")

    //Validating Regex
    val referenceRegex = Regex("[0-9][0-9][0-9][0-9]-[0-9]?[0-9]-[0-9]?[0-9]")

    val userInput = readln()
    if (!referenceRegex.matches(userInput)) {
        println("The input date is invalid")
        return askForDate()
    }

    val rowSelectedDate = userInput.split("-")

    if (
        rowSelectedDate.size > 3
    ) {
        println("The input date is invalid")
        return askForDate()
    }

    val selectedYear = rowSelectedDate.first()
    if (selectedYear.length > 4) {
        println("The input date is invalid")
        return askForDate()
    }

    val selectedMonth = rowSelectedDate[1]
    if (selectedMonth.length == 2) {
        if (!(selectedMonth.startsWith("0") ||
                    selectedMonth.startsWith("1"))
        ) {
            println("The input date is invalid")
            return askForDate()
        }
    }

    val selectedMonthInt = selectedMonth.toInt()
    if (selectedMonthInt !in 1..12) {
        println("The input date is invalid")
        return askForDate()
    }

    //date validation
    if (!isDateValid(userInput)) {
        println("The input date is invalid")
        return askForDate()
    }

    val selectedDate = rowSelectedDate.map { it.toInt() }

    return LocalDateTime(
        year = selectedDate.first(),
        monthNumber = selectedDate[1],
        dayOfMonth = selectedDate.last(),
        hour = 0,
        minute = 0
    )
}

fun isDateValid(userInput: String): Boolean {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
    dateFormatter.isLenient = false
    return try {
        dateFormatter.parse(userInput)
        true
    } catch (ex: Exception) {
        false
    }
}

fun askForPriority(): Priority {
    while (true) {
        println("Input the task priority (C, H, N, L):")
        val userInput = readln().uppercase()

        return when {
            userInput.length > 1 -> continue
            userInput == "C" -> Priority.CRITICAL
            userInput == "H" -> Priority.HIGH
            userInput == "N" -> Priority.NORMAL
            userInput == "L" -> Priority.LOW
            else -> continue
        }
    }
}