package com.example.shopapi.repository;

import com.example.shopapi.model.Item;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class ItemRepository {

    private final List<Item> items = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(idCounter.getAndIncrement());
            items.add(item);
        } else {
            // Update existing item
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(item.getId())) {
                    items.set(i, item);
                    break;
                }
            }
        }
        return item;
    }

    public Optional<Item> findById(Long id) {
        return items.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    public List<Item> findAll() {
        return new ArrayList<>(items);
    }

    public void deleteById(Long id) {
        items.removeIf(item -> item.getId().equals(id));
    }

    public boolean existsById(Long id) {
        return items.stream().anyMatch(item -> item.getId().equals(id));
    }

    public List<Item> searchByName(String name) {
        return items.stream()
                .filter(item -> item.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Item> findByCategory(String category) {
        return items.stream()
                .filter(item -> item.getCategory() != null &&
                        item.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
}

