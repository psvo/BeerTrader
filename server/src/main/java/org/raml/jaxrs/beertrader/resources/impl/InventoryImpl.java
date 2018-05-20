package org.raml.jaxrs.beertrader.resources.impl;

import org.raml.jaxrs.beertrader.data.BeerObject;
import org.raml.jaxrs.beertrader.data.InventoryObject;
import org.raml.jaxrs.beertrader.model.*;
import org.raml.jaxrs.beertrader.resources.UsersUserIdInventory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created. There, you have it.
 */
@Component
@Transactional
public class InventoryImpl extends BaseResource<InventoryObject, InventoryEntry> implements UsersUserIdInventory {

    private final EntityManager context;

    @Inject
    public InventoryImpl(EntityManager context) {
        super(InventoryEntryProperties.class, InventoryObject::new, InventoryEntryImpl::new);
        this.context = context;
    }

    @Override
    public GetUsersInventoryByUserIdResponse getUsersInventoryByUserId(String userId) {

        List<InventoryObject> inventoryObjects = context.createQuery("from  InventoryObject ", InventoryObject.class).getResultList();
        List<InventoryEntry> beers = inventoryObjects.stream().map(this::inventoryObjectToInventory).collect(Collectors.toList());

        return GetUsersInventoryByUserIdResponse.respond200WithApplicationJson(beers);
    }

    @Override
    public PostUsersInventoryByUserIdResponse postUsersInventoryByUserId(String userId, InventoryEntry entity) {
        InventoryObject inventoryObject = inventoryToInventoryObject(entity, new InventoryObject());

        try {
            BeerObject beerObject = context.createQuery("from InventoryObject b where b.id = :id", BeerObject.class).getSingleResult();
            entity.setBeerReference(beerObject.getId());
            context.persist(inventoryObject);
            return PostUsersInventoryByUserIdResponse.respond201WithApplicationJson(entity);
        } catch (NoResultException e) {

            return PostUsersInventoryByUserIdResponse.respond201WithApplicationJson(entity);
        }
    }

    @Override
    public GetUsersInventoryByUserIdAndEntryIdResponse getUsersInventoryByUserIdAndEntryId(String userId, String entryId) {
        try {
            InventoryObject inventoryObject = context.createQuery("from InventoryObject inventory where inventory.id = :id", InventoryObject.class).setParameter("id", userId).getSingleResult();
            return GetUsersInventoryByUserIdAndEntryIdResponse.respond200WithApplicationJson(inventoryObjectToInventory(inventoryObject));
        } catch (NoResultException e) {

            return GetUsersInventoryByUserIdAndEntryIdResponse.respond404();
        }
    }

    @Override
    public DeleteUsersInventoryByUserIdAndEntryIdResponse deleteUsersInventoryByUserIdAndEntryId(String userId, String entryId) {
        try {
            InventoryObject inventoryObject = context.createQuery("from InventoryObject inventory where inventory.id = :id", InventoryObject.class).setParameter("id", userId).getSingleResult();
            context.remove(inventoryObject);
            return DeleteUsersInventoryByUserIdAndEntryIdResponse.respond200();
        } catch (NoResultException e) {

            return DeleteUsersInventoryByUserIdAndEntryIdResponse.respond404();
        }
    }

    @Override
    public PutUsersInventoryByUserIdAndEntryIdResponse putUsersInventoryByUserIdAndEntryId(String userId, String entryId, InventoryEntry entity) {
        try {
            InventoryObject inventoryObject = context.createQuery("from InventoryObject user where user.id = :id", InventoryObject.class).setParameter("id", userId).getSingleResult();
            inventoryObject = inventoryToInventoryObject(entity, inventoryObject);
            context.persist(inventoryObject);
            return PutUsersInventoryByUserIdAndEntryIdResponse.respond200();
        } catch (NoResultException e) {

            return PutUsersInventoryByUserIdAndEntryIdResponse.respond404();
        }
    }

    private  InventoryEntry inventoryObjectToInventory(InventoryObject db) {
        InventoryEntry inventoryEntry = this.dbToTransfer(db);
        inventoryEntry.setBeerReference(db.getId());

        return inventoryEntry;
    }

    private  InventoryObject inventoryToInventoryObject(InventoryEntry inventoryEntry, InventoryObject inventoryObject) {

        this.transferToDB(inventoryEntry, inventoryObject);

        BeerObject beerObject = context.createQuery("from BeerObject beer where beer.id = :id", BeerObject.class).setParameter("id", inventoryEntry.getBeerReference()).getSingleResult();
        inventoryObject.setBeer(beerObject);

        return inventoryObject;
    }

}
