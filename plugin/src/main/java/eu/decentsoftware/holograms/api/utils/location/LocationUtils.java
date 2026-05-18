package eu.decentsoftware.holograms.api.utils.location;

import eu.decentsoftware.holograms.api.utils.Common;
import eu.decentsoftware.holograms.api.utils.Log;
import eu.decentsoftware.holograms.api.utils.exception.LocationParseException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;

@UtilityClass
public class LocationUtils {

    public static String asString(@NonNull Location l, boolean includeYawPitch) {
        String location = String.format("%s:%.3f:%.3f:%.3f", l.getWorld().getName(), l.getX(), l.getY(), l.getZ());
        if (includeYawPitch) {
            location += String.format(":%.3f:%.3f", l.getYaw(), l.getPitch());
        }
        return location;
    }

    public static @Nullable Location asLocation(String string) {
        return asLocation(string, ":");
    }

    public static @Nullable Location asLocation(String string, String separator) {
        try {
            return asLocationE(string, separator);
        } catch (LocationParseException e) {
            Log.warn( "Error while parsing Location %s", e, string);
            return null;
        }
    }

    public static Location asLocationE(String string) throws LocationParseException {
        return asLocationE(string, ":");
    }

    public static Location asLocationE(String string, String separator) throws LocationParseException {
        if (string == null || string.trim().isEmpty()) return null;
        String[] spl = string.replace(",", ".").split(separator);
        ParsedLocation parsedLocation = parseLocationParts(spl, separator);
        if (parsedLocation != null) {
            World world = getWorld(parsedLocation.worldName);
            if (world != null) {
                try {
                    Location location = new Location(
                            world,
                            Double.parseDouble(parsedLocation.x),
                            Double.parseDouble(parsedLocation.y),
                            Double.parseDouble(parsedLocation.z)
                    );
                    if (parsedLocation.yaw != null && parsedLocation.pitch != null) {
                        location.setYaw(Float.parseFloat(parsedLocation.yaw));
                        location.setPitch(Float.parseFloat(parsedLocation.pitch));
                    }
                    return location;
                } catch (NumberFormatException e) {
                    Log.warn("Error while parsing Location %s", e, string);
                }
            }
            throw new LocationParseException(String.format("World '%s' not found.", parsedLocation.worldName), LocationParseException.Reason.WORLD, parsedLocation.worldName);
        }
        throw new LocationParseException(String.format("Wrong location format: %s", string));
    }

    private static @Nullable ParsedLocation parseLocationParts(@NonNull String[] parts, @NonNull String separator) {
        if (parts.length < 4) {
            return null;
        }

        if (parts.length >= 6) {
            ParsedLocation withYawPitch = parseLocationParts(parts, separator, 5);
            if (withYawPitch != null) {
                return withYawPitch;
            }
        }

        return parseLocationParts(parts, separator, 3);
    }

    private static @Nullable ParsedLocation parseLocationParts(@NonNull String[] parts, @NonNull String separator, int numericParts) {
        int worldParts = parts.length - numericParts;
        if (worldParts < 1) {
            return null;
        }

        for (int index = worldParts; index < parts.length; index++) {
            if (!isNumber(parts[index])) {
                return null;
            }
        }

        String worldName = String.join(separator, Arrays.copyOfRange(parts, 0, worldParts));
        String x = parts[worldParts];
        String y = parts[worldParts + 1];
        String z = parts[worldParts + 2];
        String yaw = numericParts == 5 ? parts[worldParts + 3] : null;
        String pitch = numericParts == 5 ? parts[worldParts + 4] : null;
        return new ParsedLocation(worldName, x, y, z, yaw, pitch);
    }

    private static boolean isNumber(@NonNull String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static Location randomizeLocation(@NonNull Location location) {
        return location.add(
                Common.randomFloat() - 0.5D,
                Common.randomFloat() - 0.5D,
                Common.randomFloat() - 0.5D
        );
    }

    public static double distance2D(@NonNull Location location1, @NonNull Location location2) {
        return Math.sqrt(NumberConversions.square(location1.getX() - location2.getX()) + NumberConversions.square(location1.getZ() - location2.getZ()));
    }

    // Plugins like GHolo use the world's UUID instead of the name for location.
    private static World getWorld(@NonNull String value) {
        World world = Bukkit.getWorld(value);
        if (world != null) {
            return world;
        }

        try {
            UUID uuid = UUID.fromString(value);
            world = Bukkit.getWorld(uuid);
        } catch (IllegalArgumentException ignored) {}

        // World was neither retrieved from name nor UUID. How is this possible?
        if (world == null) {
            Log.warn("Cannot retrieve World from value %s! It's neither a valid name nor UUID.", value);
        }

        return world;
    }

    private static class ParsedLocation {

        private final String worldName;
        private final String x;
        private final String y;
        private final String z;
        private final String yaw;
        private final String pitch;

        private ParsedLocation(@NonNull String worldName, @NonNull String x, @NonNull String y, @NonNull String z, @Nullable String yaw, @Nullable String pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

    }

}
