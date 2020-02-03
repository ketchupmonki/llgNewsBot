/*
MainActivity.java
Copyright (C) 2020 Alexander Theulings, ketchupcomputing.com <alexander@theulings.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Documentation for this file can be found at https://ketchupcomputing.com/llg-news-bot/
*/

package green.livelife.llgnewsbotmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ListView theList;
    List<String> listItems = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    int reqType = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        theList = findViewById(R.id.mainList);
        theList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                itemClickByCurrentRequestType((String)parent.getItemAtPosition(position));
            }
        });
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        theList.setAdapter(arrayAdapter);

        new networkListRequest().execute();
    }

    private void itemClickByCurrentRequestType(String value){
        switch(reqType){
            case -1:
                itemClickByName(value);
                break;

            case 2:
                reqType = -1;
                listItems.clear();
                listItems.add(value);
                listItems.add("");
                listItems.add("Approve Item");
                listItems.add("");
                listItems.add("Reject Item");
                arrayAdapter.notifyDataSetChanged();
                break;

            case 3:
                reqType = -1;
                listItems.clear();
                listItems.add(value);
                listItems.add("");
                listItems.add("Reject Item");
                arrayAdapter.notifyDataSetChanged();
                break;

            case 4:
                reqType = -1;
                listItems.clear();
                listItems.add(value);
                listItems.add("");
                listItems.add("Approve Item");
                arrayAdapter.notifyDataSetChanged();
                break;
        }
    }

    private void itemClickByName(String value){
        switch(value){
            case "Unreviewed Items":
                reqType = 2;
                new networkListRequest().execute();
                break;

            case "Approved Items":
                reqType = 3;
                new networkListRequest().execute();
                break;

            case "Rejected Items (Last 10)":
                reqType = 4;
                new networkListRequest().execute();
                break;

            case "Posted Items (Last 10)":
                reqType = 5;
                new networkListRequest().execute();
                break;

            case "Log Out":
                SharedPreferences sp = getSharedPreferences(globals.sharedPrefStr, MODE_PRIVATE);
                sp.edit().putString("u", "").commit();
                sp.edit().putString("p", "").commit();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                MainActivity.this.finish();
                break;

            case "Approve Item":
                reqType = 8;
                new networkUpdateStatus().execute();
                break;

            case "Reject Item":
                reqType = 9;
                new networkUpdateStatus().execute();
                break;

            default:
                if(value.startsWith("ID:")){
                    //Open link in browser
                    String openURL = listItems.get(0).split("\\n")[2].split(" ")[1];
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(openURL));
                    startActivity(browserIntent);
                }
                break;
        }
    }

    public void openMenu(View view) {
        reqType = -1;
        listItems.clear();
        listItems.add("Unreviewed Items");
        listItems.add("Approved Items");
        listItems.add("Rejected Items (Last 10)");
        listItems.add("Posted Items (Last 10)");
        listItems.add("-------");
        listItems.add("Log Out");
        arrayAdapter.notifyDataSetChanged();
    }

    class networkListRequest extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... params) {
            List<String> results = new ArrayList<>();
            try {
                JSONObject json = globals.newJson(reqType);
                String result = globals.postToProvider(json.toString());
                JSONObject resultJson = new JSONObject(result);

                JSONArray item = resultJson.getJSONArray("items");
                for (int i = 0; i < item.length(); i++) {
                    JSONObject obj = new JSONObject(item.getString(i));
                    results.add("ID: " + obj.getString("id") + "\n" +
                            "Title: " + new String(Base64.getDecoder().decode(obj.getString("title"))) + "\n" +
                            "Link: " + new String(Base64.getDecoder().decode(obj.getString("link"))));
                }
            } catch (JSONException e) {
                Log.e("JSON ERROR", e.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return results;
        }

        @Override
        protected void onPostExecute(List<String> results) {
            listItems.clear();
            for (int i = 0; i < results.size(); i++){
                listItems.add(results.get(i));
            }
            arrayAdapter.notifyDataSetChanged();
        }
    }

    private class networkUpdateStatus extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject json = globals.newJson(reqType);
                json.put("id", listItems.get(0).split("\\n")[0].split(" ")[1]);
                return globals.postToProvider(json.toString());
            } catch (JSONException e) {
                Log.e("JSON ERROR", e.toString());
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            }
            return globals.errorString;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.equals("Executed")){
                //return to unreviewed list
                reqType = 2;
                new networkListRequest().execute();
            }else{
                listItems.add("ERROR UPDATING STATUS:");
                listItems.add(result);
                arrayAdapter.notifyDataSetChanged();
            }
        }
    }
}
