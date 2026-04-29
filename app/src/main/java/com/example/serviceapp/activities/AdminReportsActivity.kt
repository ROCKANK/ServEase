package com.example.serviceapp.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.google.firebase.database.FirebaseDatabase

class AdminReportsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTxt: TextView
    private val reportList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_admin_reports, null)
        setBaseView(view)

        recyclerView = findViewById(R.id.recyclerView)
        emptyTxt = findViewById(R.id.emptyTxt)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val titleTxt: TextView = v.findViewById(R.id.titleTxt)
                val reasonTxt: TextView = v.findViewById(R.id.reasonTxt)
                val emailTxt: TextView = v.findViewById(R.id.emailTxt)
                val statusTxt: TextView = v.findViewById(R.id.statusTxt)
            }
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.viewholder_report, parent, false)
                return VH(v)
            }
            override fun getItemCount() = reportList.size
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val report = reportList[position]
                val vh = holder as VH
                vh.titleTxt.text = report["title"]?.toString() ?: ""
                vh.reasonTxt.text = report["reason"]?.toString() ?: ""
                vh.emailTxt.text = report["userEmail"]?.toString() ?: ""
                val status = report["status"]?.toString() ?: "Open"
                vh.statusTxt.text = status
                vh.statusTxt.setBackgroundResource(
                    if (status == "Resolved") R.drawable.status_completed_bg
                    else R.drawable.status_pending_bg
                )

                vh.itemView.setOnClickListener {
                    AlertDialog.Builder(this@AdminReportsActivity)
                        .setTitle(report["title"]?.toString())
                        .setMessage(
                            "From: ${report["userEmail"]}\n" +
                                    "Reason: ${report["reason"]}\n\n" +
                                    "${report["body"]}"
                        )
                        .setPositiveButton("Mark Resolved") { _, _ ->
                            val reportId = report["reportId"]?.toString() ?: return@setPositiveButton
                            FirebaseDatabase.getInstance().reference
                                .child("Reports").child(reportId)
                                .child("status").setValue("Resolved")
                                .addOnSuccessListener {
                                    reportList[position] = report.toMutableMap().apply {
                                        put("status", "Resolved")
                                    }
                                    notifyItemChanged(position)
                                    Toast.makeText(this@AdminReportsActivity,
                                        "Marked as Resolved", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }
            }
        }

        recyclerView.adapter = adapter
        loadReports(adapter)
    }

    private fun loadReports(adapter: RecyclerView.Adapter<*>) {
        FirebaseDatabase.getInstance().reference.child("Reports").get()
            .addOnSuccessListener { snapshot ->
                reportList.clear()
                for (snap in snapshot.children) {
                    @Suppress("UNCHECKED_CAST")
                    val report = snap.value as? Map<String, Any> ?: continue
                    reportList.add(report)
                }
                adapter.notifyDataSetChanged()
                emptyTxt.visibility = if (reportList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show()
            }
    }
}