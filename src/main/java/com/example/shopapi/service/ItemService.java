package com.example.shopapi.service;

import com.example.shopapi.exception.ItemNotFoundException;
import com.example.shopapi.model.Item;
import com.example.shopapi.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Item addItem(Item item) {
        return itemRepository.save(item);
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Item updateItem(Long id, Item itemDetails) {
        Item existingItem = getItemById(id);
        existingItem.setName(itemDetails.getName());
        existingItem.setDescription(itemDetails.getDescription());
        existingItem.setPrice(itemDetails.getPrice());
        existingItem.setCategory(itemDetails.getCategory());
        existingItem.setInStock(itemDetails.getInStock());
        return itemRepository.save(existingItem);
    }

    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        itemRepository.deleteById(id);
    }

    public List<Item> searchByName(String name) {
        return itemRepository.searchByName(name);
    }

    public List<Item> filterByCategory(String category) {
        return itemRepository.findByCategory(category);
    }
}

