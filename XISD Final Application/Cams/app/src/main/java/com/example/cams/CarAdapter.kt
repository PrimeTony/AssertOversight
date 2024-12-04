package com.example.cams

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CarAdapter(
    private val context: Context,
    private val carList: MutableList<Car>,  // MutableList to support item removal
    private val carIds: MutableList<String> // Store Firestore document IDs for each car
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class CarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val carNameTextView: TextView = itemView.findViewById(R.id.carNameTextView)
        val numberPlateTextView: TextView = itemView.findViewById(R.id.numberPlateTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.car_item, parent, false)
        return CarViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = carList[position]
        val carId = carIds[position]

        holder.carNameTextView.text = car.carName
        holder.numberPlateTextView.text = "Number Plate: ${car.numberPlate}"
        holder.locationTextView.text = "Location: ${car.latitude}, ${car.longitude}"

        // Delete button action
        holder.deleteButton.setOnClickListener {
            db.collection("users")
                .document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                .collection("cars")
                .document(carId)
                .delete()
                .addOnSuccessListener {
                    carList.removeAt(position)
                    carIds.removeAt(position)
                    notifyItemRemoved(position)
                    Toast.makeText(context, "Car deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to delete car", Toast.LENGTH_SHORT).show()
                }
        }

        // Edit button action
        holder.editButton.setOnClickListener {
            showEditDialog(car, carId, position)
        }
    }

    override fun getItemCount() = carList.size

    // Show edit dialog
    private fun showEditDialog(car: Car, carId: String, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_car, null)
        val carNameEditText: EditText = dialogView.findViewById(R.id.carNameEditText)
        val numberPlateEditText: EditText = dialogView.findViewById(R.id.numberPlateEditText)

        carNameEditText.setText(car.carName)
        numberPlateEditText.setText(car.numberPlate)

        AlertDialog.Builder(context)
            .setTitle("Edit Car")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newCarName = carNameEditText.text.toString()
                val newNumberPlate = numberPlateEditText.text.toString()

                // Update Firestore
                db.collection("users")
                    .document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    .collection("cars")
                    .document(carId)
                    .update(
                        mapOf(
                            "carName" to newCarName,
                            "numberPlate" to newNumberPlate
                        )
                    )
                    .addOnSuccessListener {
                        carList[position] = car.copy(carName = newCarName, numberPlate = newNumberPlate)
                        notifyItemChanged(position)
                        Toast.makeText(context, "Car updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update car", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
