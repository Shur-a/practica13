package com.example.expenses

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.room.Room
import com.example.expenses.data.ContactDataBase
import com.example.expenses.data.DATABASE_NAME
import com.example.expenses.data.models.Contacts
import com.example.expenses.data.models.JobsTypes
import com.example.expenses.databinding.ActivityMainBinding
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    var jobTypeCon: Boolean = true
    var jobType: Boolean = true
    private var pos: Int = -1
    private var expId: UUID? = null
    private var typeExpId: UUID? = null
    private var db: ContactDataBase? = null
    private var executor = Executors.newSingleThreadExecutor()
    private lateinit var binding: ActivityMainBinding
    private var conList: MutableList<Contacts> = mutableListOf()
    private var jobTypeList: MutableList<JobsTypes> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(this, ContactDataBase::class.java, DATABASE_NAME).build()
        pos = intent.getIntExtra("pos", -1)
        val uuidText = intent.getStringExtra("uuid")
        val uuidTypeText = intent.getStringExtra("uuidType")
        converterUUID(uuidText, uuidTypeText)
        getContact()
        selectEditText()
        binding.addButton.setOnClickListener{
            if (pos == -1) {
                binding.addButton.text = "Добавить"
                if (binding.editTextName.text.toString() != ""
                    && binding.editTextCost.text.toString() != ""
                    && binding.editTextDesc.text.toString() != "") {
                    addExpenses(binding.editTextName.text.toString(),
                        binding.editTextCost.text.toString().toInt(),
                        binding.editTextDesc.text.toString())
                    jobType=false
                    jobTypeCon=false

                }
                else {
                    Toast.makeText(this, "Нельзя добавить пустую строку", Toast.LENGTH_SHORT).show()
                }
            }
            else if(pos>-1){
                if (binding.editTextName.text.toString() != ""
                    && binding.editTextCost.text.toString() != ""
                    && binding.editTextDesc.text.toString() != ""){
                    executor.execute {
                        db?.contactDAO()?.updateTypeJob(
                            JobsTypes(typeExpId!!, binding.editTextDesc.text.toString())
                        )
                        db?.contactDAO()?.updateContact(
                            Contacts(expId!!,
                                binding.editTextName.text.toString(),
                                binding.editTextCost.text.toString().toInt(),
                                typeExpId!!)
                        )
                    }
                    Toast.makeText(this, "Значения изменены", Toast.LENGTH_SHORT).show()
                    super.onBackPressed()
                }
                else{
                    Toast.makeText(this, "Нельзя добавить пустую строку", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.showButton.setOnClickListener{
            val intent = Intent(this, ShowActivity::class.java)
            startActivity(intent)
        }
        binding.delete.setOnClickListener{
            executor.execute {
                db?.contactDAO()?.deleteContact(
                    Contacts(expId!!,
                        binding.editTextName.text.toString(),
                        binding.editTextCost.text.toString().toInt(),
                        typeExpId!!)
                )
                db?.contactDAO()?.deleteType(
                    JobsTypes(typeExpId!!, binding.editTextDesc.text.toString())
                )
            }
            Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show()
            binding.delete.isEnabled = false
            super.onBackPressed()
        }
    }

    private fun converterUUID(uuidExpensesText: String?, uuidTypeText: String?){
        if(uuidExpensesText != null && uuidTypeText != null){
            expId = UUID.fromString(uuidExpensesText)
            typeExpId = UUID.fromString(uuidTypeText)
        }
    }

    private fun selectEditText(){
        if(pos > -1){
            binding.addButton.text = "Изменить"
            binding.delete.visibility = View.VISIBLE
            binding.delete.isEnabled = true
            executor.execute {
                val expenses = db?.contactDAO()?.getContact(expId!!)
                val typeName = db?.contactDAO()?.getTypeContactName(typeExpId!!)

                runOnUiThread(Runnable {
                    kotlin.run {
                        binding.editTextName.setText(expenses?.nameContact)
                        binding.editTextCost.setText(expenses?.numberContact.toString())
                        binding.editTextDesc.setText(typeName)
                    }
                })
            }
        }
        else {
            binding.delete.visibility = View.INVISIBLE
            binding.addButton.text = "Добавить"
        }


    }
    private fun getContact() {
        conList.clear()
        jobTypeList.clear()
        db?.contactDAO()?.getAllJob()?.observe(this, androidx.lifecycle.Observer {
                    jobTypeList.addAll(it)
        })
        db?.contactDAO()?.getAllContact()?.observe(this, androidx.lifecycle.Observer {
                    conList.addAll(it)
        })
    }
    override fun onResume() {
        super.onResume()
        jobType  = false
        jobTypeCon  = false
        getContact()
    }
    private fun addExpenses(name: String, cost: Int, nameType:String)
    {val uuidType = UUID.randomUUID()
        val Jods = JobsTypes(uuidType, nameType)
        val con=Contacts(UUID.randomUUID(), name, cost,uuidType)

        conList.forEach(){
            if (it.nameContact==con.nameContact){
                jobType=true
            }
        }

        jobTypeList.forEach{
            if (it.typesJob == Jods.typesJob){
                jobTypeCon = true
            }
        }
        if(!jobTypeCon&&!jobType) {
            executor.execute {
                db?.contactDAO()?.addTypeJob(
                    JobsTypes(uuidType, nameType)
                )
                db?.contactDAO()?.addContact(
                    Contacts(UUID.randomUUID(), name, cost, uuidType)
                )
            }
        }else if(!jobTypeCon&&jobType){
            executor.execute {
                val uuid=db?.contactDAO()?.getTypeJobName(nameType)
                db?.contactDAO()?.addContact(
                    Contacts(UUID.randomUUID(),name,cost,uuid!!)
                )
            }
        }
        else
        {
            Toast.makeText(this, "Уже есть", Toast.LENGTH_SHORT).show()
        }
    }
}