package com.example.playerfinderapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.playerfinderapp.R;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ListView popularGamesList;
    private ImageButton searchButton, profileButton, chatButton;
    private TextView emptyStateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        popularGamesList = findViewById(R.id.popular_games_list);
        searchButton = findViewById(R.id.search_button);
        profileButton = findViewById(R.id.profile_button);
        chatButton = findViewById(R.id.chat_button);
        emptyStateView = findViewById(R.id.empty_state_view);

        setupListeners();

        popularGamesList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedGame = (String) parent.getItemAtPosition(position);

            if (selectedGame != null && !selectedGame.isEmpty()) {
                Intent intent = new Intent(HomeActivity.this, GameUsersActivity.class);
                intent.putExtra("GAME_NAME", selectedGame);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Invalid game selection.", Toast.LENGTH_SHORT).show();
            }
        });

        loadPopularGames();
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> {
            Intent searchIntent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(searchIntent);
        });

        profileButton.setOnClickListener(v -> {
            Intent profileIntent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
        });

        chatButton.setOnClickListener(v -> {
            Intent chatIntent = new Intent(HomeActivity.this, ChatListActivity.class);
            startActivity(chatIntent);
        });
    }

    private void loadPopularGames() {
        List<String> gameNames = new ArrayList<>();
        gameNames.add("ROBLOX");
        gameNames.add("Minecraft");
        gameNames.add("Fortnite");
        gameNames.add("Counter-Strike 2 & GO");
        gameNames.add("Call of Duty: Modern Warfare II/III/Warzone 2.0");
        gameNames.add("The Sims 4");
        gameNames.add("League of Legends");
        gameNames.add("Valorant");
        gameNames.add("Grand Theft Auto V");
        gameNames.add("Overwatch 1 & 2");
        gameNames.add("Rocket League");
        gameNames.add("World of Warcraft");
        gameNames.add("Tom Clancy's Rainbow Six: Siege");
        gameNames.add("Cyberpunk 2077");
        gameNames.add("Diablo IV");
        gameNames.add("Apex Legends");
        gameNames.add("EA Sports FC 25");
        gameNames.add("Genshin Impact");
        gameNames.add("Escape From Tarkov");
        gameNames.add("BeamNG.drive");

        if (gameNames.isEmpty()) {
            popularGamesList.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, gameNames);
            popularGamesList.setAdapter(adapter);
            emptyStateView.setVisibility(View.GONE);
            popularGamesList.setVisibility(View.VISIBLE);
        }
    }
}
