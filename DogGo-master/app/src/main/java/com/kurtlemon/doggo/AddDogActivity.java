package com.kurtlemon.doggo;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class AddDogActivity extends AppCompatActivity {

    // Permissions and requests
    private final int PHOTO_CAPTURE_REQUEST = 1;
    private final int WRITE_EXTERNAL_REQUEST = 2;
    private final int SELECT_FILE = 3;

    // Firebase storage instances and references
    private FirebaseStorage dogStorage;
    private StorageReference gsReference;

    // Firebase database information
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    // Firebase user information
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;

    // ID variables
    private String userID;
    private String dogID;

    // Photo Path
    private String mCurrentPhotoPath;

    private Bitmap dogPhotoBitmap;

    // Displayed XML entities
    private ImageButton dogImageButton;
    private Button uploadButton;
    private EditText nameEditText;
    private EditText descriptionEditText;
    private ProgressBar uploadProgressBar;

    // Variables used for functionality
    private boolean imageSelected;
    private boolean editingDog;
    private boolean keepImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_dog);

        // Firebase authentication set up
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        userID = user.getUid();

        //Fields if the user is editing their dog instead of adding a new one
        final Intent intent = getIntent();
        if(intent.getStringExtra("id") != null){
            dogID = intent.getStringExtra("id");
            editingDog = true;
            imageSelected = true;
            keepImage = true;
        }else{
            createDogID();
            editingDog = false;
            imageSelected = false;
            keepImage = false;
        }

        // Firebase
        dogStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference()
                .child("dogInfo").child(userID).child(dogID);

        // Setting up functionality of xml attributes and filling if editing an existing dog
        dogImageButton = findViewById(R.id.DogImageButton);
        // Fills in the image button if editing a dog
        if(editingDog){
            // Setup for the storage
            final String storageURL = "gs://doggo-38323.appspot.com/doggo/" + userID + "/" +
                    dogID + ".png";
            gsReference = dogStorage.getReferenceFromUrl(storageURL);
            gsReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // Load the image into the image view
                    Glide.with(getApplicationContext()).load(uri).override(300, 300)
                            .into(dogImageButton);


                }
            });
        }
        dogImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectAnImage();
            }
        });
        nameEditText = findViewById(R.id.nameEditText);
        nameEditText.setText(intent.getStringExtra("name"));
        descriptionEditText = findViewById(R.id.descriptionEditText);
        descriptionEditText.setText(intent.getStringExtra("description"));
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        uploadProgressBar.setVisibility(View.INVISIBLE);
        uploadButton = findViewById(R.id.SaveNewDogButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If all the information has been filled out, then readyForUpload is true
                boolean readyForUpload = true;
                if(nameEditText.getText().toString().equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter a name for your dog.",
                            Toast.LENGTH_SHORT).show();
                    readyForUpload = false;
                }
                if(descriptionEditText.getText().toString().equals("")){
                    Toast.makeText(getApplicationContext(), "Please tell us about your dog.",
                            Toast.LENGTH_SHORT).show();
                    readyForUpload = false;
                }
                if(!imageSelected){
                    Toast.makeText(getApplicationContext(),
                            "Please attach a photo of your dog.", Toast.LENGTH_SHORT).show();
                    readyForUpload = false;
                }
                if(readyForUpload){
                    // Notify the user the photo is uploading
                    Toast.makeText(getApplicationContext(),
                            "Uploading your photo! This may take a minute",
                            Toast.LENGTH_SHORT).show();

                    // If the user is uploading a new image
                    if(!keepImage) {
                        // Save the photo as a byte array to upload
                        ByteArrayOutputStream dogPicByteArrayOutputStream =
                                new ByteArrayOutputStream();
                        dogPhotoBitmap.compress(Bitmap.CompressFormat.PNG, 100,
                                dogPicByteArrayOutputStream);
                        byte[] uploadData = dogPicByteArrayOutputStream.toByteArray();

                        // Create the file path for upload
                        String filePath = "doggo/" + userID + "/" + dogID + ".png";
                        StorageReference dogStorageReference = dogStorage.getReference(filePath);

                        // Create an instance of the DogInfo and push it to the database.
                        DogInfo dogInfo = new DogInfo(dogID,
                                nameEditText.getText().toString(),
                                descriptionEditText.getText().toString());
                        databaseReference.setValue(dogInfo);

                        // Metadata to be stored with the image
                        StorageMetadata dogIDMetadata = new StorageMetadata.Builder()
                                .setCustomMetadata("Dog ID", dogID)
                                .build();

                        // UI controls, make things visible to let the user know it's working
                        uploadProgressBar.setVisibility(View.VISIBLE);
                        uploadButton.setEnabled(false);

                        // Create the upload task for sending the photo
                        UploadTask dogUploadTask = dogStorageReference.putBytes(uploadData,
                                dogIDMetadata);
                        dogUploadTask.addOnSuccessListener(AddDogActivity.this,
                                new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // Revert the UI to its original state and exit the activity
                                        uploadProgressBar.setVisibility(View.INVISIBLE);
                                        uploadButton.setEnabled(true);
                                        AddDogActivity.this.finish();
                                    }
                                });
                    }else{
                        // If the user is not uploading a new image
                        // Create an instance of the DogInfo and push it to the database.
                        DogInfo dogInfo = new DogInfo(dogID,
                                nameEditText.getText().toString(),
                                descriptionEditText.getText().toString());
                        databaseReference.setValue(dogInfo);
                        AddDogActivity.this.finish();
                    }
                }
            }
        });
    }

    /**
     * Behavior after a photo is taken or when the user selects a photo from the gallery.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == PHOTO_CAPTURE_REQUEST && resultCode == RESULT_OK){
            galleryAddPic();
            setImageButtonPic();
            imageSelected = true;
        } else if (requestCode == SELECT_FILE && resultCode == RESULT_OK){
            Uri selectedImageURI = data.getData();
            try {
                dogPhotoBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                        selectedImageURI);
                dogImageButton.setImageURI(selectedImageURI);
            } catch (IOException ioe) {
                Toast.makeText(getApplicationContext(), "Error Selecting Photo.",
                        Toast.LENGTH_SHORT).show();
            }
            imageSelected = true;
        }
    }

    /**
     * Behavior after requesting permissions for writing external files.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Try again
                takeAndSavePicture();
            }
            else {
                Toast.makeText(this, "We need permission!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Adds a photo to the gallery and notifies it that it's data has been changed.
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * Checks permissions and starts the picture taking.
     */
    private void takeAndSavePicture(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_REQUEST);
        } else {
            dispatchTakePictureIntent();
        }
    }

    /**
     * Creates the file that the image will be stored in.
     *
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "DogGo_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Launches the intent for taking the picture and passes in the necessary info for saving the
     * photo.
     */
    private void dispatchTakePictureIntent(){
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ioEx) {
                Toast.makeText(getApplicationContext(), "Error taking photo.",
                        Toast.LENGTH_SHORT);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.kurtlemon.doggo", photoFile);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePhotoIntent, PHOTO_CAPTURE_REQUEST);
            }
        }
    }

    /**
     * Creates an AlertDialog that prompts the user to choose an image from the gallery or to take a
     * new one.
     */
    private void selectAnImage(){
        final String[] options = {"Take a Photo", "Choose an Existing Photo", "Cancel"};
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(AddDogActivity.this);
        alertDialogBuilder.setTitle("Upload a Photo");
        alertDialogBuilder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch(options[i]){
                    case "Take a Photo":
                        takeAndSavePicture();
                        break;
                    case "Choose an Existing Photo":
                        Intent intent1 = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent1.setType("image/*");
                        startActivityForResult(intent1.createChooser(intent1, "Select File"),
                                SELECT_FILE);
                        break;
                    default:
                        dialogInterface.dismiss();
                        break;
                }
            }
        });
        alertDialogBuilder.show();
    }

    /**
     * After a photo has been taken, assigns the ImageButton to the taken photo.
     */
    private void setImageButtonPic() {
        // Get the dimensions of the View
        int targetW = dogImageButton.getWidth();
        int targetH = dogImageButton.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        dogPhotoBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        dogImageButton.setImageBitmap(dogPhotoBitmap);
        keepImage = false;
    }

    /**
     * Assigns DogID to be a (most likely) unique ID for the dog
     */
    private void createDogID(){
        dogID = "" + UUID.randomUUID();
    }
}
