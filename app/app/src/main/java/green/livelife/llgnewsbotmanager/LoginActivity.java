/*
LoginActivity.java
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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        globals.displayWarning(this);
    }

    public void processLogin(View view) {
        EditText unameField = findViewById(R.id.username);
        EditText pwordField = findViewById(R.id.password);

        SharedPreferences sp = getSharedPreferences(globals.sharedPrefStr, MODE_PRIVATE);
        sp.edit().putString("u", unameField.getText().toString()).commit();
        sp.edit().putString("p", pwordField.getText().toString()).commit();

        new globals.testLogin().execute(this);
    }
}