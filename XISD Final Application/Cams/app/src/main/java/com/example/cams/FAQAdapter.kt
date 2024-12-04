package com.example.cams



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cams.R

class FAQAdapter(private val faqList: List<FAQItem>) : RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.faq_item, parent, false)
        return FAQViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val currentFAQ = faqList[position]
        holder.questionTextView.text = currentFAQ.question
        holder.answerTextView.text = currentFAQ.answer

        // Handle click event to toggle visibility of the answer
        holder.itemView.setOnClickListener {
            if (holder.answerTextView.visibility == View.VISIBLE) {
                holder.answerTextView.visibility = View.GONE
                holder.expandIcon.setImageResource(R.drawable.ic_arrow_down)
            } else {
                holder.answerTextView.visibility = View.VISIBLE
                holder.expandIcon.setImageResource(R.drawable.ic_arrow_up)
            }
        }
    }

    override fun getItemCount(): Int {
        return faqList.size
    }

    inner class FAQViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionTextView: TextView = itemView.findViewById(R.id.questionTextView)
        val answerTextView: TextView = itemView.findViewById(R.id.answerTextView)
        val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
    }
}
