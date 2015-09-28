package blogreader.com.example.android.blogreader;


import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainListActivity extends ListActivity {


    public static final int NUMBER_OF_POSTS = 20;
    public static final String TAG = MainListActivity.class.getSimpleName();
    protected JSONObject mBlogData;
    protected ProgressBar mProgressBar;
    private final String   KEY_TITLE="title";
    private final String   KEY_AUTHOR="author";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);

        if(isNetworkAvaliable()){
            mProgressBar.setVisibility(View.VISIBLE);
            GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
            getBlogPostsTask.execute();//calls Asynchronus class's doInbackGround
        }else{
            Toast.makeText(this, "Network Unavailable!", Toast.LENGTH_LONG).show();
        }

        //Toast.makeText(this, getString(R.string.no_items), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        try {
            JSONArray jsonPosts=mBlogData.getJSONArray("posts");
            JSONObject jsonPost=jsonPosts.getJSONObject(position);
            String blogURL=jsonPost.getString("url");
            Intent intent;
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(blogURL));
            startActivity(intent);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAvaliable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo= manager.getActiveNetworkInfo();

        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }

        return isAvailable;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_list, menu);
        return true;
    }

    private void handleBlogResponse()
    {

        mProgressBar.setVisibility(View.INVISIBLE);
        if (mBlogData == null)
        {
            // TODO: handle errors
            updateDisplayForError();

        }
        else
        {
            try {
            JSONArray jsonPosts = mBlogData.getJSONArray("posts");
                ArrayList<HashMap<String, String>> blogPosts =
                        new ArrayList<HashMap<String, String>>();

            for (int i = 0; i < jsonPosts.length(); i++) {
                JSONObject post = jsonPosts.getJSONObject(i);
                String title = post.getString(KEY_TITLE);
                title= Html.fromHtml(title).toString();//check escape characters
                String author = post.getString(KEY_AUTHOR);
                author = Html.fromHtml(author).toString();

                HashMap<String,String> blogPost=new HashMap<String,String>();
                blogPost.put(KEY_TITLE,title);
                blogPost.put(KEY_AUTHOR, author);
                blogPosts.add(blogPost);//addding hashmap object to arraylist

            }//debugging2 to see if hashmap obects are added properly to arraylist
                for (HashMap<String, String> map : blogPosts)
                    for (Map.Entry<String, String> mapEntry : map.entrySet())
                    {
                        String key = mapEntry.getKey();
                        String value = mapEntry.getValue();
                        Log.v(TAG,"Debugging2" +key);
                        Log.v(TAG,"Debugging2" +value);
                    }//debuggging2 to see if haspmaps has proper values

                String[] keys={KEY_TITLE,KEY_AUTHOR};
                int [] ids={android.R.id.text1,android.R.id.text2};

                SimpleAdapter adapter =new SimpleAdapter(this,blogPosts,android.R.layout.simple_list_item_2,keys,ids);
                setListAdapter(adapter);
                onContentChanged();

        }
            catch (JSONException e)
            {
            Log.e(TAG, "Exception Caught", e);

        }
    }
    }

    private void updateDisplayForError() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.error_title));
        builder.setMessage(getString(R.string.error_message));
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialogue=builder.create();
        dialogue.show();
        TextView emptyTextView= (TextView) getListView().getEmptyView();
        emptyTextView.setText(getString(R.string.no_items));
    }

    private class GetBlogPostsTask extends AsyncTask<Object, Void,JSONObject> {

        @Override
        protected JSONObject doInBackground(Object... arg0) {
            int responseCode = -1;
            JSONObject jsonResponse=null;
            try {
                URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);
                HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
                connection.connect();

                responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    Reader reader = new InputStreamReader(inputStream);
                    int nextCharacter; // read() returns an int, we cast it to char later
                    String responseData = "";
                    while(true){ // Infinite loop, can only be stopped by a "break" statement
                        nextCharacter = reader.read(); // read() without parameters returns one character
                        if(nextCharacter == -1) // A return value of -1 means that we reached the end
                            break;
                        responseData += (char) nextCharacter; // The += operator appends the character to the end of the string
                    }

                    jsonResponse = new JSONObject(responseData);
                    String status = jsonResponse.getString("status");
                    Log.v(TAG, status);

                    JSONArray jsonPosts = jsonResponse.getJSONArray("posts");
                    for (int i = 0; i < jsonPosts.length(); i++)
                    {//just for debugging
                        JSONObject jsonPost = jsonPosts.getJSONObject(i);
                        String title = jsonPost.getString("title");
                        Log.v(TAG, "Post " + i + ": " + title);
                    }
                }
                else {
                    Log.i(TAG, "Unsuccessful HTTP Response Code: " + responseCode);
                }


            }
            catch (MalformedURLException e) {
                Log.e(TAG, "URLException caught: "+e, e);
            }
            catch (IOException e) {
                Log.e(TAG, "IOException caught: "+e, e);
            }
            catch (Exception e) {
                Log.e(TAG, "Exception caught: "+e, e);
            }

            return jsonResponse;
        }
        @Override
        protected void onPostExecute(JSONObject result){
            mBlogData=result;//This class is a bridge between AsyncTask class and Main
            handleBlogResponse();

        }
    }



}