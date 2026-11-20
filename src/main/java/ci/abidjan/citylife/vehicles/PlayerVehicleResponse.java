package ci.abidjan.citylife.vehicles;

import java.util.UUID;

public record PlayerVehicleResponse(
        UUID id,
        String vehicleCode,
        String name,
        String brand,
        Vehicle.VehicleType vehicleType,
        String licensePlate,
        String colorHex,
        Integer fuelLevel,
        Integer engineHealth,
        boolean locked,
        boolean stored,
        String district,
        Double posX,
        Double posY,
        Double posZ
) {
    public static PlayerVehicleResponse from(PlayerVehicle pv) {
        Vehicle v = pv.getVehicle();
        return new PlayerVehicleResponse(
                pv.getId(),
                v.getVehicleCode(),
                v.getName(),
                v.getBrand(),
                v.getVehicleType(),
                pv.getLicensePlate(),
                pv.getColorHex(),
                pv.getFuelLevel(),
                pv.getEngineHealth(),
                pv.isLocked(),
                pv.isStored(),
                pv.getDistrict(),
                pv.getPosX(),
                pv.getPosY(),
                pv.getPosZ()
        );
    }
}